package com.aistudio.sepatify.domain.repository

import com.aistudio.sepatify.domain.model.DownloadedSong
import com.aistudio.sepatify.data.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloadedSongs(): Flow<List<DownloadedSong>>
    suspend fun downloadSong(song: Song)
    suspend fun deleteDownload(songId: String)
    fun isDownloaded(songId: String): Flow<Boolean>
    fun getDownloadProgress(songId: String): Flow<Float?>
    fun getActiveDownloads(): Flow<Map<String, Float>>
    suspend fun getLocalPlaybackUri(song: Song): String?
}
