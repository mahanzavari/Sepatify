package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aistudio.sepatify.data.local.ChatMessageEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.remote.dto.ProfileDto
import com.aistudio.sepatify.data.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val mainViewModel: MainViewModel
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredUsers: StateFlow<List<String>> = _searchQuery
        .flatMapLatest { chatRepository.searchUsers(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followedUsers: StateFlow<List<String>> = chatRepository.getFollowedUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentConversations: StateFlow<List<String>> = chatRepository.getRecentConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val onlineUsers: StateFlow<Set<String>> = chatRepository.getOnlineUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // --- NEW: Dynamic User Social Network Management ---
    private val _viewedProfileUsername = MutableStateFlow<String?>(null)
    private val _refreshNetworkTrigger = MutableStateFlow(0)

    private val _isNetworkRefreshing = MutableStateFlow(false)
    val isNetworkRefreshing: StateFlow<Boolean> = _isNetworkRefreshing.asStateFlow()

    /** Tell the ViewModel which profile to target. Pass null for Current Logged-in User. */
    fun viewProfile(username: String?) {
        _viewedProfileUsername.value = username
    }

    fun refreshNetwork() {
        viewModelScope.launch {
            _isNetworkRefreshing.value = true
            _refreshNetworkTrigger.value += 1
            delay(500) // Aesthetic delay for smooth UI feedback
            _isNetworkRefreshing.value = false
        }
    }

    val viewedProfile: StateFlow<ProfileDto?> = _viewedProfileUsername
        .flatMapLatest { un -> if (un == null) flowOf(null) else chatRepository.getProfileFlow(un) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val networkFollowers: StateFlow<List<ProfileDto>> = combine(_viewedProfileUsername, _refreshNetworkTrigger) { un, _ -> un }
        .flatMapLatest { un ->
            if (un == null) chatRepository.getFollowersProfiles(null)
            else chatRepository.getProfileFlow(un).flatMapLatest { profile ->
                if (profile != null) chatRepository.getFollowersProfiles(profile.id) else flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkFollowing: StateFlow<List<ProfileDto>> = combine(_viewedProfileUsername, _refreshNetworkTrigger) { un, _ -> un }
        .flatMapLatest { un ->
            if (un == null) chatRepository.getFollowingProfiles(null)
            else chatRepository.getProfileFlow(un).flatMapLatest { profile ->
                if (profile != null) chatRepository.getFollowingProfiles(profile.id) else flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewedProfileStats: StateFlow<Pair<Int, Int>> = combine(_viewedProfileUsername, _refreshNetworkTrigger) { un, _ -> un }
        .flatMapLatest { un ->
            if (un == null) chatRepository.getFollowStats(null)
            else chatRepository.getProfileFlow(un).flatMapLatest { profile ->
                if (profile != null) chatRepository.getFollowStats(profile.id) else flowOf(0 to 0)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    // --- End Network ---

    fun trackPresence() {
        viewModelScope.launch { chatRepository.trackPresence() }
    }

    fun untrackPresence() {
        viewModelScope.launch { chatRepository.untrackPresence() }
    }

    fun getProfile(username: String): Flow<ProfileDto?> = chatRepository.getProfileFlow(username)

    fun isFollowing(username: String): Flow<Boolean> = chatRepository.isFollowing(username)

    fun toggleFollow(username: String) {
        viewModelScope.launch { chatRepository.toggleFollowUser(username) }
    }

    fun getMessagesForUser(otherUser: String): Flow<List<ChatMessageEntity>> = chatRepository.getMessages(otherUser)

    private val pagedMessagesCache = mutableMapOf<String, Flow<PagingData<ChatMessageEntity>>>()
    fun getMessagesPaged(otherUser: String): Flow<PagingData<ChatMessageEntity>> {
        return pagedMessagesCache.getOrPut(otherUser) {
            chatRepository.getMessagesPaged(otherUser).cachedIn(viewModelScope)
        }
    }

    fun getTypingState(otherUser: String): Flow<Boolean> = chatRepository.getTypingState(otherUser)

    fun setTyping(otherUser: String, isTyping: Boolean) {
        viewModelScope.launch { chatRepository.setTyping(otherUser, isTyping) }
    }

    fun sendMessage(otherUser: String, text: String, songShare: Song? = null) {
        viewModelScope.launch { chatRepository.sendMessage(otherUser, text, songShare) }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
}