package com.aistudio.sepatify.domain.usecase.search

import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class SearchSongsUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(query: String): Flow<List<Song>> =
        songRepository.searchSongs(query)
}
