package com.aistudio.sepatify.domain.model

data class ChatMessage(
    val id: Long = 0,
    val remoteId: Long? = null,
    val senderName: String,
    val receiverName: String,
    val text: String,
    val isSongShare: Boolean,
    val songId: String?,
    val songTitle: String?,
    val songArtist: String?,
    val songCover: String?,
    val songAudio: String?,
    val timestamp: Long,
    val status: String
)
