package com.aistudio.sepatify.domain.usecase.playlist

import com.aistudio.sepatify.domain.repository.SongRepository

class UpdatePlaylistCategoryUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(playlistId: Long, category: String) =
        songRepository.updatePlaylistCategory(playlistId, category)
}
