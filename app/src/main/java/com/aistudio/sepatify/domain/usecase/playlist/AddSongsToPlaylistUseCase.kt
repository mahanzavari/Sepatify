package com.aistudio.sepatify.domain.usecase.playlist

import com.aistudio.sepatify.domain.repository.SongRepository

class AddSongsToPlaylistUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(playlistId: Long, songIds: List<String>): Result<Unit> =
        songRepository.addSongsToPlaylist(playlistId, songIds)
}
