package com.aistudio.sepatify.domain.usecase.playlist

import com.aistudio.sepatify.domain.model.Playlist
import com.aistudio.sepatify.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetUserPlaylistsUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<Playlist>> = songRepository.getUserPlaylists()
}
