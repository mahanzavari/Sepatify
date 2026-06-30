package com.example.data.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.DownloadedSongDao
import com.example.data.local.DownloadedSongEntity
import com.example.data.model.Song
import com.example.download.DownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadedSongDao: DownloadedSongDao,
    private val workManager: WorkManager
) : DownloadRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getDownloadedSongs(): Flow<List<DownloadedSongEntity>> {
        return downloadedSongDao.getDownloadedSongs()
    }

    override suspend fun downloadSong(song: Song) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_SONG_ID to song.id,
                    DownloadWorker.KEY_AUDIO_URL to song.audioUrl
                )
            )
            .addTag(DownloadWorker.TAG_DOWNLOAD)
            .addTag(DownloadWorker.songTag(song.id))
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorker.uniqueWorkName(song.id),
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )

        // Persist the Room row once the background download finishes successfully.
        repoScope.launch {
            var finished = false
            while (!finished) {
                val info = workManager.getWorkInfosForUniqueWork(DownloadWorker.uniqueWorkName(song.id)).get()
                    .firstOrNull() ?: break
                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val localPath = info.outputData.getString(DownloadWorker.KEY_LOCAL_PATH)
                            ?: DownloadWorker.localFileFor(context, song.id).absolutePath
                        downloadedSongDao.insertDownloadedSong(
                            DownloadedSongEntity(
                                id = song.id,
                                title = song.title,
                                artistName = song.artistName,
                                coverImageUrl = song.coverImageUrl,
                                audioUrl = song.audioUrl,
                                localFilePath = localPath
                            )
                        )
                        finished = true
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> finished = true
                    else -> kotlinx.coroutines.delay(300)
                }
            }
        }
    }

    override suspend fun deleteDownload(songId: String) {
        workManager.cancelUniqueWork(DownloadWorker.uniqueWorkName(songId))
        DownloadWorker.localFileFor(context, songId).let { if (it.exists()) it.delete() }
        downloadedSongDao.deleteDownloadedSongById(songId)
    }

    override fun isDownloaded(songId: String): Flow<Boolean> {
        return downloadedSongDao.isDownloadedFlow(songId)
    }

    override fun getActiveDownloads(): Flow<Map<String, Float>> {
        return workManager.getWorkInfosByTagFlow(DownloadWorker.TAG_DOWNLOAD).map { infos ->
            infos.filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                .mapNotNull { info ->
                    val songId = info.tags.firstOrNull { it.startsWith(DownloadWorker.SONG_TAG_PREFIX) }
                        ?.removePrefix(DownloadWorker.SONG_TAG_PREFIX) ?: return@mapNotNull null
                    songId to info.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f).coerceIn(0f, 0.99f)
                }
                .toMap()
        }
    }

    override fun getDownloadProgress(songId: String): Flow<Float?> {
        return workManager.getWorkInfosForUniqueWorkFlow(DownloadWorker.uniqueWorkName(songId)).map { infos ->
            val info = infos.firstOrNull() ?: return@map null
            when (info.state) {
                WorkInfo.State.SUCCEEDED -> null
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> null
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                    val progress = info.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f)
                    progress.coerceIn(0f, 0.99f)
                }
                else -> null
            }
        }
    }

    override suspend fun getLocalPlaybackUri(song: Song): String? {
        val downloaded = downloadedSongDao.isDownloadedFlow(song.id).first()
        if (!downloaded) return null
        val file = DownloadWorker.localFileFor(context, song.id)
        return if (file.exists()) file.absolutePath else null
    }
}
