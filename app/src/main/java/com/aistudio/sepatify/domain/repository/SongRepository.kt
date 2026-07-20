package com.aistudio.sepatify.domain.repository

import androidx.paging.PagingData
import com.aistudio.sepatify.domain.model.Playlist
import com.aistudio.sepatify.data.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getTrendingSongs(): Flow<List<Song>>
    fun getDailyRecommendations(): Flow<List<Song>>
    fun getNewReleases(): Flow<List<Song>>
    fun getExclusiveSongs(): Flow<List<Song>>
    fun getMostPopular(): Flow<List<Song>>
    fun getLocalPlaylists(): Flow<List<Song>>
    fun getGlobalPlaylists(): Flow<List<Song>>

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
    suspend fun deleteRecentSong(songId: String)

    fun getUserPlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(title: String, description: String, category: String, isPrivate: Boolean = false): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addSongToPlaylist(playlistId: Long, songId: String)
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<String>): Result<Unit>
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)
    fun getSongsForPlaylist(playlistId: Long, category: String): Flow<List<Song>>
    fun getSongsForPlaylistPaged(playlistId: Long, category: String): Flow<PagingData<Song>>
    suspend fun updatePlaylistCategory(playlistId: Long, category: String)
}
