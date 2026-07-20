package com.aistudio.sepatify.domain.usecase.download

import com.aistudio.sepatify.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class GetDownloadProgressUseCase(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(songId: String): Flow<Float?> =
        downloadRepository.getDownloadProgress(songId)
}
