package com.example.data.repository

import androidx.paging.PagingData
import com.example.data.local.ChatMessageEntity
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(otherUser: String): Flow<List<ChatMessageEntity>>
    fun getMessagesPaged(otherUser: String): Flow<PagingData<ChatMessageEntity>>
    fun getTypingState(otherUser: String): Flow<Boolean>
    suspend fun sendMessage(otherUser: String, text: String, songShare: Song? = null)
    suspend fun setTyping(otherUser: String, isTyping: Boolean)
    fun getFollowedUsers(): Flow<List<String>>
    fun getFollowers(): Flow<List<String>>
    suspend fun toggleFollowUser(username: String)
    fun isFollowing(username: String): Flow<Boolean>
    fun searchUsers(query: String): Flow<List<String>>
}
