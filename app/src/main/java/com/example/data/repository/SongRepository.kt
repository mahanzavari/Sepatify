package com.example.data.repository

import androidx.paging.PagingData
import com.example.data.model.Song
import com.example.data.local.PlaylistEntity
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getTrendingSongs(): Flow<List<Song>>
    fun getDailyRecommendations(): Flow<List<Song>>
    fun getNewReleases(): Flow<List<Song>>
    fun getMostPopular(): Flow<List<Song>>
    fun getLocalPlaylists(): Flow<List<Song>>
    fun getGlobalPlaylists(): Flow<List<Song>>

    /** One-shot refresh of the `songs` catalog from Supabase into the local Room cache. */
    suspend fun refreshCatalog()

    fun searchSongs(query: String): Flow<List<Song>>
    fun searchSongsPaged(query: String): Flow<PagingData<Song>>

    suspend fun addSearchHistory(query: String)
    fun getSearchHistory(): Flow<List<String>>
    suspend fun deleteSearchHistoryItem(query: String)
    suspend fun clearSearchHistory()

    fun getLikedSongs(): Flow<List<Song>>
    suspend fun toggleLikeSong(song: Song)
    fun isSongLiked(songId: String): Flow<Boolean>
    fun getRecentlyPlayedSongs(): Flow<List<Song>>
    suspend fun addRecentSong(song: Song)

    // Playlists (Global playlists come from Supabase; User playlists are owned by the signed-in user)
    fun getUserPlaylists(): Flow<List<PlaylistEntity>>
    suspend fun createPlaylist(title: String, description: String, category: String): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addSongToPlaylist(playlistId: Long, songId: String)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)
    fun getSongsForPlaylist(playlistId: Long, category: String): Flow<List<Song>>
    fun getSongsForPlaylistPaged(playlistId: Long, category: String): Flow<PagingData<Song>>
}
