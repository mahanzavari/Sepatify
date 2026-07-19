package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.sepatify.data.local.DownloadedSongEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.repository.DownloadRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadViewModel(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _sortType = MutableStateFlow("date")
    val sortType: StateFlow<String> = _sortType.asStateFlow()

    private val _activeDownloadIds = MutableStateFlow<Set<String>>(emptySet())
    private val progressFlows = mutableMapOf<String, StateFlow<Float?>>()

    val downloadedSongs: StateFlow<List<DownloadedSongEntity>> = downloadRepository.getDownloadedSongs()
        .combine(_sortType) { list, sort ->
            when (sort) {
                "title" -> list.sortedBy { it.title }
                "artist" -> list.sortedBy { it.artistName }
                else -> list.sortedByDescending { it.timestamp }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSort(sortBy: String) {
        _sortType.value = sortBy
    }

    fun downloadProgressFor(songId: String): StateFlow<Float?> {
        return progressFlows.getOrPut(songId) {
            downloadRepository.getDownloadProgress(songId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        }
    }

    val activeDownloads: StateFlow<Map<String, Float>> = downloadRepository.getActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun initiateDownload(song: Song) {
        viewModelScope.launch {
            _activeDownloadIds.value = _activeDownloadIds.value + song.id
            downloadRepository.downloadSong(song)
            _activeDownloadIds.value = _activeDownloadIds.value - song.id
        }
    }

    fun removeDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(songId)
        }
    }

    // === MVI central event handler ===
    fun onEvent(event: DownloadEvent) {
        when (event) {
            is DownloadEvent.UpdateSort       -> updateSort(event.sortBy)
            is DownloadEvent.InitiateDownload -> initiateDownload(event.song)
            is DownloadEvent.RemoveDownload   -> removeDownload(event.songId)
        }
    }
}