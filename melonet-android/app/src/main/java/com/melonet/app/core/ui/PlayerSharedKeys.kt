package com.melonet.app.core.ui

/**
 * Shared transition keys for mini-player / now-playing handoff (wired in A6).
 */
object PlayerSharedKeys {
    fun songCover(songId: String): String = "song_cover_$songId"
}
