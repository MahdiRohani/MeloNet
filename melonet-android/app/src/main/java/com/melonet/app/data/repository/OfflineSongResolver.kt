package com.melonet.app.data.repository

/**
 * Resolves local file paths for offline playback (wired in A8 Downloads).
 */
interface OfflineSongResolver {
    suspend fun localPathFor(songId: String): String?
}

class NoOpOfflineSongResolver : OfflineSongResolver {
    override suspend fun localPathFor(songId: String): String? = null
}
