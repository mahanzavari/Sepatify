package com.aistudio.sepatify.domain.usecase.download

import com.aistudio.sepatify.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class GetActiveDownloadsUseCase(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(): Flow<Map<String, Float>> = downloadRepository.getActiveDownloads()
}
