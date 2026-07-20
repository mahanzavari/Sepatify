package com.aistudio.sepatify.domain.usecase.playlist

import com.aistudio.sepatify.domain.repository.SongRepository

/**
 * Composite UseCase: creates a playlist and adds songs atomically.
 * If adding songs fails, the playlist is rolled back (deleted).
 */
class CreatePlaylistWithSongsUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        isPrivate: Boolean,
        songIds: List<String>
    ): Result<Long> {
        val playlistId = songRepository.createPlaylist(title, description, "User", isPrivate)
        if (playlistId == -1L) return Result.failure(Exception("Failed to create playlist record"))

        val addResult = songRepository.addSongsToPlaylist(playlistId, songIds)
        if (addResult.isFailure) {
            songRepository.deletePlaylist(playlistId)
            return Result.failure(addResult.exceptionOrNull() ?: Exception("Unknown error"))
        }
        return Result.success(playlistId)
    }
}
