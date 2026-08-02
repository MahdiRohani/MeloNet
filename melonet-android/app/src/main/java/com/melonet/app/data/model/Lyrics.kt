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

    fun lineIndexAt(positionMs: Long, durationMs: Long = 0L, offsetMs: Long = 0L): Int {
        if (lines.isEmpty()) return -1
        val effective = positionMs + offsetMs
        return if (synced) {
            var index = -1
            for (i in lines.indices) {
                if (lines[i].timeMs <= effective) index = i else break
            }
            index
        } else {
            if (durationMs <= 0) 0
            else ((positionMs.toFloat() / durationMs) * lines.size)
                .toInt()
                .coerceIn(0, lines.lastIndex)
        }
    }

    companion object {
        val EMPTY = Lyrics(emptyList(), synced = false)
    }
}
