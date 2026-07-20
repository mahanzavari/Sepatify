package com.aistudio.sepatify.domain.usecase.playlist

import com.aistudio.sepatify.domain.repository.SongRepository

class CreatePlaylistUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        category: String,
        isPrivate: Boolean = false
    ): Long = songRepository.createPlaylist(title, description, category, isPrivate)
}
