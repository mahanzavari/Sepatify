package com.aistudio.sepatify.domain.usecase.playlist

import androidx.paging.PagingData
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class GetSongsForPlaylistUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(playlistId: Long, category: String): Flow<List<Song>> =
        songRepository.getSongsForPlaylist(playlistId, category)
}

class GetSongsForPlaylistPagedUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(playlistId: Long, category: String): Flow<PagingData<Song>> =
        songRepository.getSongsForPlaylistPaged(playlistId, category)
}
