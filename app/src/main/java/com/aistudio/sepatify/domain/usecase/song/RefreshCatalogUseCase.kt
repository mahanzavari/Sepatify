package com.aistudio.sepatify.domain.usecase.song

import com.aistudio.sepatify.domain.repository.SongRepository

class RefreshCatalogUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke() = songRepository.refreshCatalog()
}
