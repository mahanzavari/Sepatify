package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aistudio.sepatify.data.local.ChatMessageEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.repository.ChatRepository
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

    // Paging 3 backed chat history (FR: chat history must use Paging 3).
    private val pagedMessagesCache = mutableMapOf<String, Flow<PagingData<ChatMessageEntity>>>()
    fun getMessagesPaged(otherUser: String): Flow<PagingData<ChatMessageEntity>> {
        return pagedMessagesCache.getOrPut(otherUser) {
            chatRepository.getMessagesPaged(otherUser).cachedIn(viewModelScope)
        }
    }

    fun getTypingState(otherUser: String): Flow<Boolean> {
        return chatRepository.getTypingState(otherUser)
    }

    fun sendMessage(otherUser: String, text: String, songShare: Song? = null) {
        viewModelScope.launch {
            chatRepository.sendMessage(otherUser, text, songShare)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
