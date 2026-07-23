package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.sepatify.domain.model.Artist
import com.aistudio.sepatify.domain.model.ArtistProfile
import com.aistudio.sepatify.data.repository.ArtistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ArtistUiState {
    object Loading : ArtistUiState
    data class Success(val profile: ArtistProfile) : ArtistUiState
    data class Error(val message: String) : ArtistUiState
}

sealed interface ArtistEvent {
    data class LoadProfile(val artistId: String) : ArtistEvent
    data class ToggleFollow(val artistId: String) : ArtistEvent
    data class PlaySong(val artistId: String) : ArtistEvent
}

class ArtistViewModel(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Artist>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else artistRepository.searchArtists(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadProfile(artistId: String) {
        viewModelScope.launch {
            _uiState.value = ArtistUiState.Loading
            val profile = artistRepository.getArtistProfile(artistId)
            if (profile != null) {
                _uiState.value = ArtistUiState.Success(profile)
            } else {
                _uiState.value = ArtistUiState.Error("Failed to load artist profile")
            }
        }
    }

    fun toggleFollow(artistId: String) {
        viewModelScope.launch {
            artistRepository.toggleFollowArtist(artistId)
            // Reload profile to update state
            loadProfile(artistId)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onEvent(event: ArtistEvent) {
        when (event) {
            is ArtistEvent.LoadProfile -> loadProfile(event.artistId)
            is ArtistEvent.ToggleFollow -> toggleFollow(event.artistId)
            is ArtistEvent.PlaySong -> { /* handled by SharedAudioViewModel */ }
        }
    }
}