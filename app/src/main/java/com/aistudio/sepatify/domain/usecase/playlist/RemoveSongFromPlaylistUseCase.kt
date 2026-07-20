package com.aistudio.sepatify.domain.usecase.playlist

import com.aistudio.sepatify.domain.repository.SongRepository

class RemoveSongFromPlaylistUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(playlistId: Long, songId: String) =
        songRepository.removeSongFromPlaylist(playlistId, songId)
}
