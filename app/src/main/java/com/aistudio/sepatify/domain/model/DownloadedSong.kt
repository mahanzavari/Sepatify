package com.aistudio.sepatify.domain.model

data class DownloadedSong(
    val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val localFilePath: String,
    val timestamp: Long
)
