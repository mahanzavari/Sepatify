package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.repository.SongRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val trending: List<Song>,
        val recommendations: List<Song>,
        val newReleases: List<Song>,
        val mostPopular: List<Song>,
        val globalPlaylistSongs: List<Song> = emptyList(),
        val localPlaylistSongs: List<Song> = emptyList(),
        val exclusiveSongs: List<Song> = emptyList() // Added Exclusive
    ) : HomeUiState
}

class HomeViewModel(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    @Suppress("UNCHECKED_CAST")
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            delay(1200) // Shimmer animation rule requirement NFR-05

            combine(
                songRepository.getTrendingSongs(),
                songRepository.getDailyRecommendations(),
                songRepository.getNewReleases(),
                songRepository.getMostPopular(),
                songRepository.getGlobalPlaylists(),
                songRepository.getLocalPlaylists(),
                songRepository.getExclusiveSongs() // Added to Flow list
            ) { values ->
                HomeUiState.Success(
                    trending = values[0] as List<Song>,
                    recommendations = values[1] as List<Song>,
                    newReleases = values[2] as List<Song>,
                    mostPopular = values[3] as List<Song>,
                    globalPlaylistSongs = values[4] as List<Song>,
                    localPlaylistSongs = values[5] as List<Song>,
                    exclusiveSongs = values[6] as List<Song> // Maps to 7th flow
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}