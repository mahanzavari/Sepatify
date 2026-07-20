package com.aistudio.sepatify.domain.model

data class Playlist(
    val id: Long,
    val title: String,
    val description: String,
    val isUserCreated: Boolean,
    val category: String,
    val isPrivate: Boolean = false
)
