package com.aistudio.sepatify.data.repository

import androidx.paging.PagingData
import com.aistudio.sepatify.data.local.ChatMessageEntity
import com.aistudio.sepatify.data.local.PlaylistEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.remote.dto.ProfileDto
import kotlinx.coroutines.flow.Flow

data class UserProfileDetails(
    val followersCount: Int,
    val followingCount: Int,
    val playlists: List<PlaylistEntity>,
    val likedSongs: List<Song> = emptyList(),
    val playlistSongCounts: Map<Long, Int> = emptyMap()
)

interface ChatRepository {
    fun getRecentConversations(): Flow<List<String>>
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

    fun getOnlineUsers(): Flow<Set<String>>
    suspend fun trackPresence()
    suspend fun untrackPresence()

    fun getProfileFlow(username: String): Flow<ProfileDto?>

    // The missing method needed for the UI:
    suspend fun getUserProfileDetails(username: String): UserProfileDetails
}