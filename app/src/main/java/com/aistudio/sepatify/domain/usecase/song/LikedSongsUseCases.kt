package com.aistudio.sepatify.domain.usecase.song

import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetLikedSongsUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> = songRepository.getLikedSongs()
}

class ToggleLikeSongUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(song: Song) = songRepository.toggleLikeSong(song)
}

class IsSongLikedUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(songId: String): Flow<Boolean> = songRepository.isSongLiked(songId)
}
