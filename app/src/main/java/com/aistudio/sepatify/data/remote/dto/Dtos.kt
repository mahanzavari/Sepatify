package com.aistudio.sepatify.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ProfileUpdateDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_premium") val isPremium: Boolean? = null,
    @SerialName("bio") val bio: String? = null
)

@Serializable
data class SongDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("cover_image_url") val coverImageUrl: String,
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("category") val category: String = "Popular",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChatMessageDto(
    @SerialName("id") val id: Long,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("text") val text: String = "",
    @SerialName("is_song_share") val isSongShare: Boolean = false,
    @SerialName("song_id") val songId: String? = null,
    @SerialName("song_title") val songTitle: String? = null,
    @SerialName("song_artist") val songArtist: String? = null,
    @SerialName("song_cover") val songCover: String? = null,
    @SerialName("song_audio") val songAudio: String? = null,
    @SerialName("status") val status: String = "Sent",
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class NewChatMessageDto(
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("text") val text: String = "",
    @SerialName("is_song_share") val isSongShare: Boolean = false,
    @SerialName("song_id") val songId: String? = null,
    @SerialName("song_title") val songTitle: String? = null,
    @SerialName("song_artist") val songArtist: String? = null,
    @SerialName("song_cover") val songCover: String? = null,
    @SerialName("song_audio") val songAudio: String? = null,
    @SerialName("status") val status: String = "Sent"
)

@Serializable
data class PlaylistDto(
    @SerialName("id") val id: Long,
    @SerialName("owner_id") val ownerId: String?,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("category") val category: String = "User",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false
)

@Serializable
data class NewPlaylistDto(
    @SerialName("owner_id") val ownerId: String?,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("category") val category: String = "User",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false
)

@Serializable
data class PlaylistCategoryUpdateDto(
    @SerialName("category") val category: String
)

@Serializable
data class PlaylistSongDto(
    @SerialName("playlist_id") val playlistId: Long,
    @SerialName("song_id") val songId: String,
    @SerialName("position") val position: Int 
)

@Serializable
data class PlaylistSongJoinDto(
    @SerialName("playlist_id") val playlistId: Long = 0L,
    @SerialName("song_id") val songId: String = "",
    @SerialName("songs") val songs: SongDto
)

@Serializable
data class LikedSongDto(
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LikedSongJoinDto(
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    @SerialName("songs") val songs: SongDto
)

@Serializable
data class FollowDto(
    @SerialName("follower_id") val followerId: String,
    @SerialName("followed_id") val followedId: String,
    @SerialName("profiles") val profiles: ProfileDto? = null
)

@Serializable
data class TypingPayload(
    @SerialName("user_id") val userId: String,
    @SerialName("is_typing") val isTyping: Boolean
)