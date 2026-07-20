package com.aistudio.sepatify.domain.usecase.playlist

import com.aistudio.sepatify.domain.repository.SongRepository

class DeletePlaylistUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(playlistId: Long) = songRepository.deletePlaylist(playlistId)
}
