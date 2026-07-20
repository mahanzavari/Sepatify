package com.aistudio.sepatify.domain.usecase.search

import com.aistudio.sepatify.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow

class AddSearchHistoryUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(query: String) = songRepository.addSearchHistory(query)
}

class GetSearchHistoryUseCase(
    private val songRepository: SongRepository
) {
    operator fun invoke(): Flow<List<String>> = songRepository.getSearchHistory()
}

class DeleteSearchHistoryItemUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(query: String) = songRepository.deleteSearchHistoryItem(query)
}

class ClearSearchHistoryUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke() = songRepository.clearSearchHistory()
}
