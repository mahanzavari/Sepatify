package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aistudio.sepatify.data.local.PlaylistEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.repository.SongRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val songRepository: SongRepository
) : ViewModel() {

    // Trigger used to instantly refresh the playlist fetch after creating a new one or a folder
    private val refreshTrigger = MutableStateFlow(0)

    val userPlaylists: StateFlow<List<PlaylistEntity>> = refreshTrigger.flatMapLatest {
        songRepository.getUserPlaylists()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshPlaylists() {
        refreshTrigger.value += 1
    }

    fun createNewPlaylistWithSongs(title: String, description: String, songIds: List<String>) {
        viewModelScope.launch {
            val pid = songRepository.createPlaylist(title, description, "User")
            if (pid != -1L) {
                songIds.forEach { songId ->
                    songRepository.addSongToPlaylist(pid, songId)
                }
                refreshPlaylists()
            }
        }
    }

    fun groupPlaylistsIntoFolder(folderName: String, playlistIds: List<Long>) {
        viewModelScope.launch {
            val categoryName = "folder:$folderName"
            playlistIds.forEach { pid ->
                songRepository.updatePlaylistCategory(pid, categoryName)
            }
            refreshPlaylists()
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            songRepository.deletePlaylist(playlistId)
            refreshPlaylists()
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            songRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long, category: String): Flow<List<Song>> {
        if (playlistId == -3L) {
            return songRepository.getLikedSongs()
        }
        return songRepository.getSongsForPlaylist(playlistId, category)
    }

    fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return songRepository.getRecentlyPlayedSongs()
    }

    fun getSongsForPlaylistPaged(playlistId: Long, category: String): Flow<PagingData<Song>> {
        if (playlistId == -3L) {
            return songRepository.getLikedSongs().map { PagingData.from(it) }
        }
        return songRepository.getSongsForPlaylistPaged(playlistId, category).cachedIn(viewModelScope)
    }
}