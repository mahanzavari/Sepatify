package com.aistudio.sepatify.domain.model

import com.aistudio.sepatify.data.model.Song

data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val isPremium: Boolean = false,
    val createdAt: String? = null
)

data class UserProfileDetails(
    val followersCount: Int,
    val followingCount: Int,
    val playlists: List<Playlist>,
    val likedSongs: List<Song> = emptyList(),
    val playlistSongCounts: Map<Long, Int> = emptyMap()
)