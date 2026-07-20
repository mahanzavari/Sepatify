package com.aistudio.sepatify.domain.usecase.download

import com.aistudio.sepatify.domain.repository.DownloadRepository

class DeleteDownloadUseCase(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(songId: String) = downloadRepository.deleteDownload(songId)
}
