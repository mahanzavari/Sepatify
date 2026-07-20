package com.aistudio.sepatify.domain.usecase.download

import com.aistudio.sepatify.domain.model.DownloadedSong
import com.aistudio.sepatify.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class GetDownloadedSongsUseCase(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadedSong>> = downloadRepository.getDownloadedSongs()
}
