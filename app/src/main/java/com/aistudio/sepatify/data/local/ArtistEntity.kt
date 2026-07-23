package com.aistudio.sepatify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val verified: Boolean = false,
    val monthlyListeners: Int = 0,
    val isFollowed: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)