package com.aistudio.sepatify.data.model

import androidx.annotation.Keep

@Keep
data class Song(
    val id: String,
    val title: String,
    val artistName: String,
    val coverImageUrl: String,
    val audioUrl: String,
    val category: String = "Popular"
)
