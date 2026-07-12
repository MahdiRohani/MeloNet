package com.melonet.app.data.model

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
}

enum class DownloadSort {
    NEWEST,
    TITLE,
}

data class DownloadItem(
    val songId: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val audioUrl: String,
    val filePath: String,
    val status: DownloadStatus,
    val progress: Int,
    val downloadedAt: Long,
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        artistName = artistName,
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        category = "",
        lyrics = "",
        durationSec = 0,
    )
}
