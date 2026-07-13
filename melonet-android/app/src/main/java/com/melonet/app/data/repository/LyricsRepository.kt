package com.melonet.app.data.repository

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.data.model.Lyrics
import com.melonet.app.data.model.LyricLine
import com.melonet.app.data.remote.LyricsApi
import com.melonet.app.data.remote.dto.LrcLibResponseDto
import kotlinx.coroutines.withContext

class LyricsRepository(
    private val lyricsApi: LyricsApi,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun getLyrics(
        title: String,
        artist: String,
        durationSec: Int,
    ): Lyrics = withContext(dispatchers.io) {
        val direct = runCatching {
            lyricsApi.getLyrics(
                artistName = artist,
                trackName = title,
                durationSec = durationSec.takeIf { it > 0 },
            )
        }.getOrNull()

        val best = direct ?: runCatching {
            lyricsApi.searchLyrics("$title $artist")
                .firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                ?: lyricsApi.searchLyrics(title).firstOrNull()
        }.getOrNull()

        best?.let(::toLyrics) ?: Lyrics.EMPTY
    }

    private fun toLyrics(dto: LrcLibResponseDto): Lyrics {
        val synced = dto.syncedLyrics?.let(::parseLrc).orEmpty()
        if (synced.isNotEmpty()) {
            return Lyrics(lines = synced, synced = true)
        }
        val plain = dto.plainLyrics
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { LyricLine(timeMs = -1L, text = it) }
            .orEmpty()
        return Lyrics(lines = plain, synced = false)
    }

    private fun parseLrc(raw: String): List<LyricLine> {
        val tagRegex = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
        val result = mutableListOf<LyricLine>()
        for (line in raw.split("\n")) {
            val tags = tagRegex.findAll(line).toList()
            if (tags.isEmpty()) continue
            val text = line.substring(tags.last().range.last + 1).trim()
            if (text.isEmpty()) continue
            for (tag in tags) {
                val minutes = tag.groupValues[1].toLongOrNull() ?: 0L
                val seconds = tag.groupValues[2].toLongOrNull() ?: 0L
                val fractionRaw = tag.groupValues[3]
                val fractionMs = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> fractionRaw.toLong() * 100
                    2 -> fractionRaw.toLong() * 10
                    else -> fractionRaw.take(3).toLong()
                }
                result += LyricLine(
                    timeMs = minutes * 60_000 + seconds * 1_000 + fractionMs,
                    text = text,
                )
            }
        }
        return result.sortedBy { it.timeMs }
    }
}
