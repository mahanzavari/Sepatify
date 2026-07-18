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

    val userPlaylists: StateFlow<List<PlaylistEntity>> = songRepository.getUserPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ensure the onResult signature is exactly: (Boolean, String?) -> Unit
    fun createNewPlaylistWithSongs(title: String, description: String, songIds: List<String>, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val pid = songRepository.createPlaylist(title, description, "User")
            if (pid != -1L) {
                val result = songRepository.addSongsToPlaylist(pid, songIds)
                if (result.isFailure) {
                    // Rollback playlist creation if songs failed to add
                    songRepository.deletePlaylist(pid)
                    onResult(false, result.exceptionOrNull()?.message)
                } else {
                    onResult(true, null)
                }
            } else {
                onResult(false, "Failed to create playlist record")
            }
        }
    }

    fun groupPlaylistsIntoFolder(folderName: String, playlistIds: List<Long>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val categoryName = "folder:$folderName"
            playlistIds.forEach { pid ->
                songRepository.updatePlaylistCategory(pid, categoryName)
            }
            onComplete()
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            songRepository.deletePlaylist(playlistId)
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
        // Delegate routing seamlessly up to repository & ensure ViewModel caches it uniformly
        return songRepository.getSongsForPlaylistPaged(playlistId, category).cachedIn(viewModelScope)
    }
}