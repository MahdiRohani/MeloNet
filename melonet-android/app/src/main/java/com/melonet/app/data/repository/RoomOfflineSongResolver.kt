package com.melonet.app.data.repository

class RoomOfflineSongResolver(
    private val downloadRepository: DownloadRepository,
) : OfflineSongResolver {
    override suspend fun localPathFor(songId: String): String? =
        downloadRepository.localPathFor(songId)
}
