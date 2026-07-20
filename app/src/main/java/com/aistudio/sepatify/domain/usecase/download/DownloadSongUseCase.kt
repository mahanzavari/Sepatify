package com.aistudio.sepatify.domain.usecase.download

import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.DownloadRepository

class DownloadSongUseCase(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(song: Song) = downloadRepository.downloadSong(song)
}
