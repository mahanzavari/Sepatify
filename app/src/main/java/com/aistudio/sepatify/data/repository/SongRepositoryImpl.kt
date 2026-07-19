package com.aistudio.sepatify.data.repository

import android.content.Context
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map 
import com.aistudio.sepatify.data.local.*
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.remote.PlaylistSongsPagingSource
import com.aistudio.sepatify.data.remote.Supa
import com.aistudio.sepatify.data.remote.SongSearchPagingSource
import com.aistudio.sepatify.data.remote.dto.LikedSongDto
import com.aistudio.sepatify.data.remote.dto.NewPlaylistDto
import com.aistudio.sepatify.data.remote.dto.PlaylistDto
import com.aistudio.sepatify.data.remote.dto.PlaylistSongDto
import com.aistudio.sepatify.data.remote.dto.PlaylistSongJoinDto
import com.aistudio.sepatify.data.remote.dto.SongDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val SENTINEL_GLOBAL_ID = -1L
private const val SENTINEL_LOCAL_ID = -2L
private const val SENTINEL_LIKED_ID = -3L

class SongRepositoryImpl(
    private val context: Context,
    private val likedSongDao: LikedSongDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val songCacheDao: SongCacheDao,
    private val playlistDao: PlaylistDao,
    private val authRepository: AuthRepository
) : SongRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repoScope.launch { refreshCatalog() }
        repoScope.launch { syncLikedSongsFromRemote() }
    }

    // ---------------------------------------------------------------------
    // Catalog (Supabase `songs`, cached offline in Room `song_cache`)
    // ---------------------------------------------------------------------

    override suspend fun refreshCatalog() {
        try {
            val remote = Supa.client.from("songs")
                .select(columns = Columns.ALL) { order("title", Order.ASCENDING) }
                .decodeList<SongDto>()

            if (remote.isNotEmpty()) {
                // Clear the old cache (flushes out any old hardcoded demo songs)
                songCacheDao.clearAll()
                // Insert the real songs from Supabase Storage
                songCacheDao.insertAll(remote.map { it.toCacheEntity() })
            }
        } catch (e: Exception) {
            // Offline or backend not reachable.
            // App will seamlessly fallback to using whatever is already in the songCacheDao.
            e.printStackTrace()
        }
    }

    private fun cacheFlow(category: String? = null): Flow<List<Song>> {
        val source = if (category == null) songCacheDao.getAll() else songCacheDao.getByCategory(category)
        return source.map { list -> list.map { it.toDomain() } }
    }

    override fun getAllSongs(): Flow<List<Song>> = cacheFlow()
    override fun getTrendingSongs(): Flow<List<Song>> = cacheFlow("Trending")
    override fun getDailyRecommendations(): Flow<List<Song>> = cacheFlow("Recommendation")
    override fun getNewReleases(): Flow<List<Song>> = cacheFlow("NewRelease")
    override fun getMostPopular(): Flow<List<Song>> = cacheFlow("Popular")
    override fun getExclusiveSongs(): Flow<List<Song>> = cacheFlow("Exclusive")

    override fun getLocalPlaylists(): Flow<List<Song>> = flow {
        emit(getDeviceLocalSongs())
    }

    override fun getGlobalPlaylists(): Flow<List<Song>> = flow {
        try {
            val globalPlaylistIds = Supa.client.from("playlists")
                .select(columns = Columns.list("id")) { filter { eq("category", "Global") } }
                .decodeList<PlaylistDto>()
                .map { it.id }

            if (globalPlaylistIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val songs = Supa.client.from("playlist_songs")
                .select(columns = Columns.raw("playlist_id, song_id, songs(*)")) {
                    filter { isIn("playlist_id", globalPlaylistIds) }
                    order("position", Order.ASCENDING)
                    limit(30)
                }
                .decodeList<PlaylistSongJoinDto>()
                .map { it.songs.toDomain() }
                .distinctBy { it.id }

            emit(songs)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.catch { emit(emptyList()) }

    // ---------------------------------------------------------------------
    // Search (live Supabase query with a local-cache fallback for offline use)
    // ---------------------------------------------------------------------

    override fun searchSongs(query: String): Flow<List<Song>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        try {
            val remote = Supa.client.from("songs")
                .select(columns = Columns.ALL) {
                    filter {
                        or {
                            ilike("title", "%$query%")
                            ilike("artist_name", "%$query%")
                        }
                    }
                    limit(50)
                }
                .decodeList<SongDto>()
                .map { it.toDomain() }
            emit(remote)
        } catch (e: Exception) {
            // Offline fallback: search whatever is cached locally, plus on-device tracks.
            val localDeviceMatches = getDeviceLocalSongs().filter {
                it.title.contains(query, ignoreCase = true) || it.artistName.contains(query, ignoreCase = true)
            }
            songCacheDao.search(query).collect { cached ->
                emit((cached.map { it.toDomain() } + localDeviceMatches).distinctBy { it.id })
            }
        }
    }

    override fun searchSongsPaged(query: String): Flow<PagingData<Song>> {
        return Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            SongSearchPagingSource(query)
        }.flow
    }

    override suspend fun addSearchHistory(query: String) {
        if (query.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = query))
        }
    }

    override fun getSearchHistory(): Flow<List<String>> {
        return searchHistoryDao.getSearchHistory().map { list ->
            list.map { it.query }.distinct()
        }
    }

    override suspend fun deleteSearchHistoryItem(query: String) {
        searchHistoryDao.getSearchHistory().map { list ->
            list.filter { it.query == query }.forEach {
                searchHistoryDao.deleteSearchById(it.id)
            }
        }
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearHistory()
    }

    // ---------------------------------------------------------------------
    // Liked songs (server-synced via Supabase `liked_songs`, cached in Room for offline reads)
    // ---------------------------------------------------------------------

    override fun getLikedSongs(): Flow<List<Song>> {
        return likedSongDao.getLikedSongs().map { list ->
            list.map { entity ->
                Song(entity.id, entity.title, entity.artistName, entity.coverImageUrl, entity.audioUrl)
            }
        }
    }

    override suspend fun toggleLikeSong(song: Song) {
        val alreadyLiked = likedSongDao.isLiked(song.id)
        val uid = authRepository.currentUserId()
        if (alreadyLiked) {
            likedSongDao.deleteLikedSongById(song.id)
            if (uid != null) {
                runCatching {
                    Supa.client.from("liked_songs").delete {
                        filter {
                            eq("user_id", uid)
                            eq("song_id", song.id)
                        }
                    }
                }
            }
        } else {
            likedSongDao.insertLikedSong(
                LikedSongEntity(song.id, song.title, song.artistName, song.coverImageUrl, song.audioUrl)
            )
            if (uid != null) {
                runCatching {
                    Supa.client.from("liked_songs").insert(LikedSongDto(userId = uid, songId = song.id))
                }
            }
        }
    }

    override fun isSongLiked(songId: String): Flow<Boolean> {
        return likedSongDao.isLikedFlow(songId)
    }

    override fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return recentlyPlayedDao.getRecentSongs().map { list ->
            list.map { entity ->
                Song(entity.id, entity.title, entity.artistName, entity.coverImageUrl, entity.audioUrl)
            }
        }
    }

    override suspend fun addRecentSong(song: Song) {
        recentlyPlayedDao.insertRecentSong(
            RecentlyPlayedEntity(song.id, song.title, song.artistName, song.coverImageUrl, song.audioUrl)
        )
    }

    override suspend fun deleteRecentSong(songId: String) {
        recentlyPlayedDao.deleteRecentSongById(songId)
    }

    private suspend fun syncLikedSongsFromRemote() {
        val uid = authRepository.currentUserId() ?: return
        try {
            val liked = Supa.client.from("liked_songs")
                .select(columns = Columns.raw("user_id, song_id, songs(*)")) { filter { eq("user_id", uid) } }
                .decodeList<com.aistudio.sepatify.data.remote.dto.LikedSongJoinDto>()

            liked.forEach { row ->
                val s = row.songs
                likedSongDao.insertLikedSong(
                    LikedSongEntity(s.id, s.title, s.artistName, s.coverImageUrl, s.audioUrl)
                )
            }
        } catch (e: Exception) {
            // Offline - keep whatever liked songs are already cached locally.
        }
    }

    // ---------------------------------------------------------------------
    // Playlists (Global playlists + the signed-in user's own playlists live in Supabase)
    // ---------------------------------------------------------------------

    override fun getUserPlaylists(): Flow<List<PlaylistEntity>> {
        // 1. Sync from remote in background
        repoScope.launch {
            val uid = authRepository.currentUserId()
            try {
                val remote = Supa.client.from("playlists")
                    .select(columns = Columns.ALL) {
                        if (uid != null) {
                            filter { or { eq("category", "Global"); eq("owner_id", uid) } }
                        } else {
                            filter { eq("category", "Global") }
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<PlaylistDto>()
                    
                remote.forEach { dto ->
                    playlistDao.insertPlaylist(PlaylistEntity(
                        id = dto.id,
                        title = dto.title,
                        description = dto.description,
                        isUserCreated = dto.ownerId != null,
                        category = dto.category,
                        isPrivate = dto.isPrivate
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // 2. Return local Room DB as the single source of truth for immediate UI updates
        return playlistDao.getPlaylists()
    }

    override suspend fun createPlaylist(title: String, description: String, category: String, isPrivate: Boolean): Long {
        val uid = authRepository.currentUserId() ?: return -1L
        return try {
             val createdList = Supa.client.from("playlists")
                .insert(com.aistudio.sepatify.data.remote.dto.NewPlaylistDto(ownerId = uid, title = title, description = description, category = category)) {
                    select(columns = Columns.ALL)
                }
                .decodeList<com.aistudio.sepatify.data.remote.dto.PlaylistDto>()

                val created = createdList.firstOrNull() ?: throw Exception("Empty database response")

            playlistDao.insertPlaylist(PlaylistEntity(
                id = created.id,
                title = created.title,
                description = created.description,
                isUserCreated = true,
                category = created.category,
                isPrivate = created.isPrivate
            ))
            created.id
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        runCatching {
            Supa.client.from("playlists").delete { filter { eq("id", playlistId) } }
        }
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
        runCatching {
            Supa.client.from("playlist_songs").insert(com.aistudio.sepatify.data.remote.dto.PlaylistSongDto(playlistId, songId, position = 0))
        }
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<String>): Result<Unit> {
        val validSongIds = songIds.filter { !it.startsWith("local_") }
         if (validSongIds.isEmpty()) return Result.failure(Exception("No valid cloud songs selected"))
        
        val dtos = validSongIds.mapIndexed { index, sid ->
            com.aistudio.sepatify.data.remote.dto.PlaylistSongDto(playlistId, sid, position = index)
        }
        return try {
            Supa.client.from("playlist_songs").insert(dtos)
            validSongIds.forEach { sid ->
                playlistDao.insertSongCrossRef(PlaylistSongCrossRef(playlistId, sid))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }


    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        runCatching {
            Supa.client.from("playlist_songs").delete {
                filter {
                    eq("playlist_id", playlistId)
                    eq("song_id", songId)
                }
            }
        }
    }
    
    override suspend fun updatePlaylistCategory(playlistId: Long, category: String) {
        runCatching {
            Supa.client.from("playlists").update(com.aistudio.sepatify.data.remote.dto.PlaylistCategoryUpdateDto(category)) {
                filter { eq("id", playlistId) }
            }
        }
        playlistDao.updatePlaylistCategory(playlistId, category)
    }

    private fun getDeviceLocalSongs(): List<Song> {
        val songList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol).toString()
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val dataPath = cursor.getString(dataCol) ?: ""
                    val coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80"

                    if (dataPath.isNotEmpty()) {
                        songList.add(
                            Song(
                                id = "local_$id",
                                title = title,
                                artistName = artist,
                                coverImageUrl = coverUrl,
                                audioUrl = dataPath,
                                category = "Local"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songList.sortedBy { it.title.lowercase() }
    }

    override fun getSongsForPlaylist(playlistId: Long, category: String): Flow<List<Song>> {
        if (category == "Local" || playlistId == SENTINEL_LOCAL_ID) {
            return flow { emit(getDeviceLocalSongs()) }
        }
        if (playlistId == SENTINEL_GLOBAL_ID) {
            return getGlobalPlaylists()
        }
        if (playlistId == SENTINEL_LIKED_ID) {
            return getLikedSongs()
        }
        return flow {
            try {
                val songs = Supa.client.from("playlist_songs")
                    .select(columns = Columns.raw("playlist_id, song_id, songs(*)")) {
                        filter { eq("playlist_id", playlistId) }
                        order("position", Order.ASCENDING)
                    }
                    .decodeList<PlaylistSongJoinDto>()
                    .map { it.songs.toDomain() }
                emit(songs)
            } catch (e: Exception) {
                emit(emptyList())
            }
        }.catch { emit(emptyList()) }
    }

    override fun getSongsForPlaylistPaged(playlistId: Long, category: String): Flow<PagingData<Song>> {
        if (playlistId == SENTINEL_LIKED_ID || category == "Liked") {
            return Pager(PagingConfig(pageSize = 20)) {
                likedSongDao.getLikedSongsPaged()
            }.flow.map { pagingData ->
                pagingData.map { Song(it.id, it.title, it.artistName, it.coverImageUrl, it.audioUrl) }
            }
        }
        if (playlistId == SENTINEL_LOCAL_ID || category == "Local") {
            return Pager(PagingConfig(pageSize = 50)) {
                object : androidx.paging.PagingSource<Int, Song>() {
                    override fun getRefreshKey(state: androidx.paging.PagingState<Int, Song>): Int? = null
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
                        return try {
                            val items = getDeviceLocalSongs()
                            LoadResult.Page(data = items, prevKey = null, nextKey = null)
                        } catch (e: Exception) {
                            LoadResult.Error(e)
                        }
                    }
                }
            }.flow
        }
        if (playlistId == SENTINEL_GLOBAL_ID || category == "Global") {
            return Pager(PagingConfig(pageSize = 50)) {
                object : androidx.paging.PagingSource<Int, Song>() {
                    override fun getRefreshKey(state: androidx.paging.PagingState<Int, Song>): Int? = null
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
                        return try {
                            val items = getGlobalPlaylists().first()
                            LoadResult.Page(data = items, prevKey = null, nextKey = null)
                        } catch (e: Exception) {
                            LoadResult.Error(e)
                        }
                    }
                }
            }.flow
        }
        
        // Server-side default pagination for user playlists
        return Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            PlaylistSongsPagingSource(playlistId)
        }.flow
    }
}

private fun SongDto.toDomain() = Song(id, title, artistName, coverImageUrl, audioUrl, category)
private fun SongDto.toCacheEntity() = SongCacheEntity(id, title, artistName, coverImageUrl, audioUrl, category)
private fun SongCacheEntity.toDomain() = Song(id, title, artistName, coverImageUrl, audioUrl, category)