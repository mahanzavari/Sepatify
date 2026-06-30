package com.example.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Downloads a song's audio file to app-private storage in the background using WorkManager,
 * so the transfer survives process death / app backgrounding (FR: downloads must use WorkManager).
 */
class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_SONG_ID = "song_id"
        const val KEY_AUDIO_URL = "audio_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_LOCAL_PATH = "local_path"
        const val TAG_DOWNLOAD = "download"
        const val SONG_TAG_PREFIX = "song:"

        fun songTag(songId: String) = "$SONG_TAG_PREFIX$songId"

        fun uniqueWorkName(songId: String) = "download_song_$songId"

        fun downloadsDir(context: Context): File =
            File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }

        fun localFileFor(context: Context, songId: String): File =
            File(downloadsDir(context), "$songId.audio")
    }

    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val audioUrl = inputData.getString(KEY_AUDIO_URL) ?: return@withContext Result.failure()
        val destination = localFileFor(applicationContext, songId)

        try {
            // Local (on-device) tracks are already files - simply verify and "copy" the path.
            if (audioUrl.startsWith("/") || audioUrl.startsWith("file://")) {
                val sourcePath = audioUrl.removePrefix("file://")
                File(sourcePath).copyTo(destination, overwrite = true)
                setProgress(workDataOf(KEY_PROGRESS to 1f))
                return@withContext Result.success(workDataOf(KEY_LOCAL_PATH to destination.absolutePath))
            }

            val request = Request.Builder().url(audioUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure()
                val body = response.body ?: return@withContext Result.failure()
                val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
                var bytesCopied = 0L

                body.byteStream().use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            if (totalBytes > 0) {
                                setProgress(workDataOf(KEY_PROGRESS to (bytesCopied.toFloat() / totalBytes)))
                            }
                            bytes = input.read(buffer)
                        }
                    }
                }
            }
            setProgress(workDataOf(KEY_PROGRESS to 1f))
            Result.success(workDataOf(KEY_LOCAL_PATH to destination.absolutePath))
        } catch (e: Exception) {
            destination.delete()
            Result.failure()
        }
    }
}
