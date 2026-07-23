package com.aistudio.sepatify.domain.model

import com.aistudio.sepatify.data.model.Song

data class ArtistProfile(
    val artist: Artist,
    val songs: List<Song> = emptyList(),
    val followersCount: Int = 0,
    val isFollowed: Boolean = false
)