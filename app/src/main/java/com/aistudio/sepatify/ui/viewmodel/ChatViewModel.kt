package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aistudio.sepatify.data.local.ChatMessageEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.remote.dto.ProfileDto
import com.aistudio.sepatify.data.repository.ChatRepository
import com.aistudio.sepatify.data.repository.UserProfileDetails
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Add this sealed interface ABOVE the class ──
sealed interface FollowedUsersUiState {
    object Loading : FollowedUsersUiState
    data class Loaded(val users: List<String>) : FollowedUsersUiState
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredUsers: StateFlow<List<String>> = _searchQuery
        .flatMapLatest { chatRepository.searchUsers(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── KEEP the existing followedUsers for backward compat ──
    val followedUsers: StateFlow<List<String>> = chatRepository.getFollowedUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── ADD this new state that distinguishes Loading vs Loaded ──
    val followedUsersState: StateFlow<FollowedUsersUiState> = chatRepository.getFollowedUsers()
        .map { state -> FollowedUsersUiState.Loaded(state) as FollowedUsersUiState }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FollowedUsersUiState.Loading
        )

    val recentConversations: StateFlow<List<String>> = chatRepository.getRecentConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val onlineUsers: StateFlow<Set<String>> = chatRepository.getOnlineUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _viewedUserDetails = MutableStateFlow<UserProfileDetails?>(null)
    val viewedUserDetails: StateFlow<UserProfileDetails?> = _viewedUserDetails.asStateFlow()

    fun loadUserDetails(username: String) {
        viewModelScope.launch {
            _viewedUserDetails.value = null
            _viewedUserDetails.value = chatRepository.getUserProfileDetails(username)
        }
    }

    fun trackPresence() {
        viewModelScope.launch {
            chatRepository.trackPresence()
        }
    }

    fun untrackPresence() {
        viewModelScope.launch {
            chatRepository.untrackPresence()
        }
    }

    fun getProfile(username: String): Flow<ProfileDto?> {
        return chatRepository.getProfileFlow(username)
    }

    fun isFollowing(username: String): Flow<Boolean> {
        return chatRepository.isFollowing(username)
    }

    fun toggleFollow(username: String) {
        viewModelScope.launch {
            chatRepository.toggleFollowUser(username)
        }
    }

    fun getMessagesForUser(otherUser: String): Flow<List<ChatMessageEntity>> {
        return chatRepository.getMessages(otherUser)
    }

    private val pagedMessagesCache = mutableMapOf<String, Flow<PagingData<ChatMessageEntity>>>()
    fun getMessagesPaged(otherUser: String): Flow<PagingData<ChatMessageEntity>> {
        return pagedMessagesCache.getOrPut(otherUser) {
            chatRepository.getMessagesPaged(otherUser).cachedIn(viewModelScope)
        }
    }

    fun getTypingState(otherUser: String): Flow<Boolean> {
        return chatRepository.getTypingState(otherUser)
    }

    fun setTyping(otherUser: String, isTyping: Boolean) {
        viewModelScope.launch {
            chatRepository.setTyping(otherUser, isTyping)
        }
    }

    fun sendMessage(otherUser: String, text: String, songShare: Song? = null) {
        viewModelScope.launch {
            chatRepository.sendMessage(otherUser, text, songShare)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // === MVI central event handler ===
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.LoadUserDetails   -> loadUserDetails(event.username)
            ChatEvent.TrackPresence        -> trackPresence()
            ChatEvent.UntrackPresence      -> untrackPresence()
            is ChatEvent.ToggleFollow      -> toggleFollow(event.username)
            is ChatEvent.SetTyping         -> setTyping(event.otherUser, event.isTyping)
            is ChatEvent.SendMessage       -> sendMessage(event.otherUser, event.text, event.songShare)
            is ChatEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
        }
    }
}