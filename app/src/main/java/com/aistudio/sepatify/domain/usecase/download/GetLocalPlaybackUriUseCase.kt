package com.aistudio.sepatify.domain.usecase.download

import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.DownloadRepository

class GetLocalPlaybackUriUseCase(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(song: Song): String? = downloadRepository.getLocalPlaybackUri(song)
}
