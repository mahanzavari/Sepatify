package com.aistudio.sepatify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val localFilePath: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val isUserCreated: Boolean,
    val category: String, // "International", "Local", "User"
    val isPrivate: Boolean = false
)

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String
)

@Entity(tableName = "song_cache")
data class SongCacheEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val category: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: Long? = null, // id of the row in Supabase `chat_messages`, null while still "Sending"
    val senderName: String,
    val receiverName: String,
    val text: String,
    val isSongShare: Boolean,
    val songId: String?,
    val songTitle: String?,
    val songArtist: String?,
    val songCover: String?,
    val songAudio: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String // "Sending", "Sent", "Delivered", "Read"
)