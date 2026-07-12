package com.melonet.app.data.repository

/**
 * Resolves local file paths for offline playback (wired in A8 Downloads).
 */
interface OfflineSongResolver {
    fun localPathFor(songId: Int): String?
}

class NoOpOfflineSongResolver : OfflineSongResolver {
    override fun localPathFor(songId: Int): String? = null
}
