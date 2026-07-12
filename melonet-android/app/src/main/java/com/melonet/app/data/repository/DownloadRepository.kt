package com.melonet.app.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.data.local.DownloadDao
import com.melonet.app.data.local.DownloadStorage
import com.melonet.app.data.local.toDownloadEntity
import com.melonet.app.data.local.toDownloadItem
import com.melonet.app.data.model.DownloadItem
import com.melonet.app.data.model.DownloadSort
import com.melonet.app.data.model.DownloadStatus
import com.melonet.app.data.model.Song
import com.melonet.app.feature.downloads.DownloadWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepository(
    private val downloadDao: DownloadDao,
    private val downloadStorage: DownloadStorage,
    private val workManager: WorkManager,
    private val dispatchers: DispatchersProvider,
) {
    fun observeDownloads(sort: DownloadSort): Flow<List<DownloadItem>> =
        downloadDao.observeAll().map { entities ->
            val items = entities.map { it.toDownloadItem() }
            when (sort) {
                DownloadSort.NEWEST -> items.sortedWith(
                    compareByDescending<DownloadItem> {
                        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING
                    }.thenByDescending { it.downloadedAt },
                )
                DownloadSort.TITLE -> items.sortedBy { it.title.lowercase() }
            }
        }

    fun observeDownload(songId: String): Flow<DownloadItem?> =
        downloadDao.observeBySongId(songId).map { it?.toDownloadItem() }

    suspend fun enqueueDownload(song: Song): Result<Unit> = withContext(dispatchers.io) {
        val existing = downloadDao.getBySongId(song.id)
        if (existing?.status == DownloadStatus.COMPLETED.name) {
            val path = existing.filePath
            if (path.isNotBlank() && File(path).exists()) {
                return@withContext Result.Success(Unit)
            }
        }

        downloadDao.upsert(song.toDownloadEntity(status = DownloadStatus.PENDING))
        enqueueWork(song.id)
        Result.Success(Unit)
    }

    suspend fun deleteDownload(songId: String) = withContext(dispatchers.io) {
        workManager.cancelUniqueWork(DownloadWorker.workNameFor(songId))
        downloadStorage.deleteFile(songId)
        downloadDao.delete(songId)
    }

    suspend fun retryDownload(songId: String): Result<Unit> = withContext(dispatchers.io) {
        val entity = downloadDao.getBySongId(songId) ?: return@withContext Result.Success(Unit)
        downloadStorage.deleteFile(songId)
        downloadDao.upsert(
            entity.copy(
                status = DownloadStatus.PENDING.name,
                progress = 0,
                filePath = "",
                downloadedAt = 0L,
            ),
        )
        enqueueWork(songId)
        Result.Success(Unit)
    }

    suspend fun localPathFor(songId: String): String? = withContext(dispatchers.io) {
        val path = downloadDao.getCompletedFilePath(songId) ?: return@withContext null
        if (path.isNotBlank() && File(path).exists()) path else null
    }

    private fun enqueueWork(songId: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_SONG_ID to songId))
            .setConstraints(downloadConstraints())
            .addTag(DownloadWorker.tagFor(songId))
            .build()

        workManager.enqueueUniqueWork(
            DownloadWorker.workNameFor(songId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun downloadConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
