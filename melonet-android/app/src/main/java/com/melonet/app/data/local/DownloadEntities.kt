package com.melonet.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.melonet.app.data.model.DownloadItem
import com.melonet.app.data.model.DownloadStatus
import com.melonet.app.data.model.Song

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val audioUrl: String,
    val filePath: String = "",
    val status: String,
    val progress: Int = 0,
    val downloadedAt: Long = 0L,
)

fun Song.toDownloadEntity(
    status: DownloadStatus = DownloadStatus.PENDING,
    filePath: String = "",
    progress: Int = 0,
    downloadedAt: Long = 0L,
): DownloadEntity = DownloadEntity(
    songId = id,
    title = title,
    artistName = artistName,
    coverUrl = coverUrl,
    audioUrl = audioUrl,
    filePath = filePath,
    status = status.name,
    progress = progress,
    downloadedAt = downloadedAt,
)

fun DownloadEntity.toDownloadItem(): DownloadItem = DownloadItem(
    songId = songId,
    title = title,
    artistName = artistName,
    coverUrl = coverUrl,
    audioUrl = audioUrl,
    filePath = filePath,
    status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED),
    progress = progress,
    downloadedAt = downloadedAt,
)
