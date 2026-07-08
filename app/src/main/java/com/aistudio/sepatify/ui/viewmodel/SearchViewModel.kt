package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.repository.SongRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<Song>) : SearchUiState
}

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val songRepository: SongRepository,
    private val mainViewModel: MainViewModel // helper to retrieve language of strings if needed
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All") // "All", "Songs", "Artists"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val searchHistory: StateFlow<List<String>> = songRepository.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Paginated results (FR: search must use Paging 3 instead of loading the full result set at once).
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedResults: Flow<PagingData<Song>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .combine(_selectedFilter) { query, filterValue -> query to filterValue }
        .flatMapLatest { (query, filterValue) ->
            if (query.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                songRepository.searchSongsPaged(query).map { pagingData ->
                    if (filterValue == "Artists") {
                        pagingData.filter { it.artistName.contains(query, ignoreCase = true) }
                    } else {
                        pagingData
                    }
                }
            }
        }
        .cachedIn(viewModelScope)

    init {
        // Debounce search flow to meet: "FR-20: The search input shall use debounce() to prevent network calls on every keystroke"
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    _uiState.value = SearchUiState.Loading
                    songRepository.addSearchHistory(query)
                }
            }
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
                    songRepository.searchSongs(query)
                }
            }
            .combine(_selectedFilter) { results, filter ->
                when (filter) {
                    "Songs" -> results.filter { true } // in our case all are songs, but artist filtering applies
                    "Artists" -> results.filter { it.artistName.lowercase().contains(_searchQuery.value.lowercase()) }
                    else -> results
                }
            }
            .onEach { filtered ->
                if (_searchQuery.value.isBlank()) {
                    _uiState.value = SearchUiState.Idle
                } else {
                    _uiState.value = SearchUiState.Success(filtered)
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            songRepository.deleteSearchHistoryItem(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            songRepository.clearSearchHistory()
        }
    }
}
