package com.aistudio.sepatify.domain.model

data class Artist(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val verified: Boolean = false,
    val monthlyListeners: Int = 0,
    val isFollowed: Boolean = false
)