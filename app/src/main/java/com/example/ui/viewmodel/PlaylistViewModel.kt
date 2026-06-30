package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.local.PlaylistEntity
import com.example.data.model.Song
import com.example.data.repository.SongRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val songRepository: SongRepository
) : ViewModel() {

    val userPlaylists: StateFlow<List<PlaylistEntity>> = songRepository.getUserPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNewPlaylist(title: String, description: String, category: String = "User") {
        viewModelScope.launch {
            songRepository.createPlaylist(title, description, category)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            songRepository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            songRepository.addSongToPlaylist(playlistId, songId)
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
