package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Song
import com.example.data.repository.SongRepository
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
        val localPlaylistSongs: List<Song> = emptyList()
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

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            delay(1200) // Shimmer animation rule requirement NFR-05: "All shimmer loading animations shall appear before any list data is available"

            combine(
                songRepository.getTrendingSongs(),
                songRepository.getDailyRecommendations(),
                songRepository.getNewReleases(),
                songRepository.getMostPopular(),
                songRepository.getGlobalPlaylists(),
                songRepository.getLocalPlaylists()
            ) { values ->
                HomeUiState.Success(
                    trending = values[0],
                    recommendations = values[1],
                    newReleases = values[2],
                    mostPopular = values[3],
                    globalPlaylistSongs = values[4],
                    localPlaylistSongs = values[5]
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
