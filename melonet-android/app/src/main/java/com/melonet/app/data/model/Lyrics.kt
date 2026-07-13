package com.melonet.app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class LyricLine(
    val timeMs: Long,
    val text: String,
)

@Immutable
data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    companion object {
        val EMPTY = Lyrics(emptyList(), synced = false)
    }
}
