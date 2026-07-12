package com.melonet.app.feature.downloads

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.melonet.app.data.local.DownloadDao
import com.melonet.app.data.local.DownloadStorage
import com.melonet.app.data.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.FileOutputStream

class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val downloadDao: DownloadDao by inject()
    private val downloadStorage: DownloadStorage by inject()
    private val okHttpClient: OkHttpClient by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val songId = inputData.getString(KEY_SONG_ID) ?: return@withContext Result.failure()
        val entity = downloadDao.getBySongId(songId) ?: return@withContext Result.failure()

        val outputFile = downloadStorage.fileFor(songId)
        downloadDao.updateProgress(
            songId = songId,
            status = DownloadStatus.DOWNLOADING.name,
            progress = 0,
            filePath = "",
            downloadedAt = 0L,
        )

        try {
            val response = okHttpClient.newCall(Request.Builder().url(entity.audioUrl).build()).execute()
            if (!response.isSuccessful) {
                markFailed(songId)
                return@withContext if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            }

            val body = response.body ?: run {
                markFailed(songId)
                return@withContext Result.failure()
            }

            val totalBytes = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (isStopped) {
                            outputFile.delete()
                            markFailed(songId)
                            return@withContext Result.failure()
                        }
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val progress = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 99)
                            downloadDao.updateProgress(
                                songId = songId,
                                status = DownloadStatus.DOWNLOADING.name,
                                progress = progress,
                                filePath = "",
                                downloadedAt = 0L,
                            )
                        }
                    }
                }
            }

            downloadDao.updateProgress(
                songId = songId,
                status = DownloadStatus.COMPLETED.name,
                progress = 100,
                filePath = outputFile.absolutePath,
                downloadedAt = System.currentTimeMillis(),
            )
            Result.success()
        } catch (_: Exception) {
            outputFile.delete()
            markFailed(songId)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private suspend fun markFailed(songId: String) {
        downloadDao.updateProgress(
            songId = songId,
            status = DownloadStatus.FAILED.name,
            progress = 0,
            filePath = "",
            downloadedAt = 0L,
        )
    }

    companion object {
        const val KEY_SONG_ID = "song_id"
        private const val WORK_PREFIX = "download_"
        private const val MAX_ATTEMPTS = 3

        fun workNameFor(songId: String): String = WORK_PREFIX + songId

        fun tagFor(songId: String): String = WORK_PREFIX + songId
    }
}
