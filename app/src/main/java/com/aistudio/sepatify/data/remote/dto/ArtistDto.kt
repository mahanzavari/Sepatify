package com.aistudio.sepatify.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("monthly_listeners") val monthlyListeners: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ArtistWithSongsDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("monthly_listeners") val monthlyListeners: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("songs") val songs: List<SongDto>? = null
)