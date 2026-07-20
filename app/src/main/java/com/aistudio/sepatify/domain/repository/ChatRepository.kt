package com.aistudio.sepatify.domain.repository

import androidx.paging.PagingData
import com.aistudio.sepatify.domain.model.ChatMessage
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.model.UserProfile
import com.aistudio.sepatify.domain.model.UserProfileDetails
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getRecentConversations(): Flow<List<String>>
    fun getMessages(otherUser: String): Flow<List<ChatMessage>>
    fun getMessagesPaged(otherUser: String): Flow<PagingData<ChatMessage>>
    fun getTypingState(otherUser: String): Flow<Boolean>
    suspend fun sendMessage(otherUser: String, text: String, songShare: Song? = null)
    suspend fun setTyping(otherUser: String, isTyping: Boolean)
    fun getFollowedUsers(): Flow<List<String>>
    fun getFollowers(): Flow<List<String>>
    suspend fun toggleFollowUser(username: String)
    fun isFollowing(username: String): Flow<Boolean>
    fun searchUsers(query: String): Flow<List<String>>
    fun getOnlineUsers(): Flow<Set<String>>
    suspend fun trackPresence()
    suspend fun untrackPresence()
    fun getProfileFlow(username: String): Flow<UserProfile?>
    suspend fun getUserProfileDetails(username: String): UserProfileDetails
}
