package com.example.data.repository

import com.example.data.local.DownloadedSongEntity
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloadedSongs(): Flow<List<DownloadedSongEntity>>
    suspend fun downloadSong(song: Song)
    suspend fun deleteDownload(songId: String)
    fun isDownloaded(songId: String): Flow<Boolean>

    /** Live 0f..1f progress for an in-flight WorkManager download, or null if none is running. */
    fun getDownloadProgress(songId: String): Flow<Float?>

    /** All currently in-flight downloads (songId -> 0f..1f progress), for a global "Downloading…" summary. */
    fun getActiveDownloads(): Flow<Map<String, Float>>

    /** Returns a local `file://` URI if [song] was downloaded, otherwise null (smart routing / offline playback). */
    suspend fun getLocalPlaybackUri(song: Song): String?
}
