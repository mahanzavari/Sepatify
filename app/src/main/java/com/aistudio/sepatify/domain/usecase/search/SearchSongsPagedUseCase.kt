package com.aistudio.sepatify.domain.usecase.search

import androidx.paging.PagingData
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class SearchSongsPagedUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(query: String): Flow<PagingData<Song>> =
        songRepository.searchSongsPaged(query)
}
