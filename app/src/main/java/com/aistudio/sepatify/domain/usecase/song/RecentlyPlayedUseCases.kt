package com.aistudio.sepatify.domain.usecase.song

import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetRecentlyPlayedSongsUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<Song>> = songRepository.getRecentlyPlayedSongs()
}

class AddRecentSongUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(song: Song) = songRepository.addRecentSong(song)
}

class DeleteRecentSongUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(songId: String) = songRepository.deleteRecentSong(songId)
}
