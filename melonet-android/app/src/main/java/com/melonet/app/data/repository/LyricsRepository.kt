package com.melonet.app.data.repository

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.data.model.Lyrics
import com.melonet.app.data.model.LyricLine
import com.melonet.app.data.remote.LyricsApi
import com.melonet.app.data.remote.dto.LrcLibResponseDto
import kotlinx.coroutines.withContext
import kotlin.math.abs

class LyricsRepository(
    private val lyricsApi: LyricsApi,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun getLyrics(
        title: String,
        artist: String,
        durationSec: Int,
        album: String? = null,
        embeddedLyrics: String? = null,
        syncedOnly: Boolean = false,
    ): Lyrics = withContext(dispatchers.io) {
        // Prefer synced lyrics already stored on the song when available.
        embeddedLyrics?.takeIf { it.isNotBlank() }?.let { raw ->
            val parsed = parseLrc(raw)
            if (isMeaningfulSynced(parsed)) {
                return@withContext Lyrics(parsed, synced = true)
            }
            if (!syncedOnly && parsed.isNotEmpty()) {
                return@withContext Lyrics(parsed, synced = parsed.any { it.timeMs >= 0 })
            }
        }

        val cleanTitle = cleanTrackName(title)
        val cleanArtist = cleanArtistName(artist)
        val cleanAlbum = album?.let(::cleanTrackName).orEmpty()
        // Catalog duration is often wrong/zero — LRCLIB /api/get 404s on bad duration.
        val reliableDuration = durationSec.takeIf { it in 30..900 }

        val direct = fetchDirect(
            artist = cleanArtist,
            title = cleanTitle,
            album = cleanAlbum,
            durationSec = reliableDuration,
        )?.takeIf { hasUsableLyrics(it, syncedOnly) }

        val minScore = if (syncedOnly) 0.35f else 0.4f
        if (direct != null && scoreCandidate(direct, cleanTitle, cleanArtist, reliableDuration, syncedOnly) >= minScore) {
            val lyrics = toLyrics(direct, syncedOnly)
            if (lyrics.lines.isNotEmpty()) return@withContext lyrics
        }

        val searchHits = buildList {
            runCatching { lyricsApi.searchLyrics("$cleanTitle $cleanArtist") }.getOrNull()?.let(::addAll)
            if (isEmpty()) {
                runCatching { lyricsApi.searchLyrics(cleanTitle) }.getOrNull()?.let(::addAll)
            }
            if (isEmpty() && cleanArtist.isNotBlank()) {
                runCatching { lyricsApi.searchLyrics(cleanArtist) }.getOrNull()?.let(::addAll)
            }
        }

        val searchMin = if (syncedOnly) 0.3f else 0.35f
        val ranked = searchHits
            .asSequence()
            .filter { hasUsableLyrics(it, syncedOnly) }
            .map { it to scoreCandidate(it, cleanTitle, cleanArtist, reliableDuration, syncedOnly) }
            .filter { it.second >= searchMin }
            .sortedByDescending { it.second }
            .toList()

        val best = ranked.firstOrNull()?.first ?: direct
        best?.let { toLyrics(it, syncedOnly) }?.takeIf { it.lines.isNotEmpty() } ?: Lyrics.EMPTY
    }

    /** Fast local check — no network. True when [raw] parses as real timed LRC. */
    fun hasEmbeddedSyncedLyrics(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return isMeaningfulSynced(parseLrc(raw))
    }

    private suspend fun fetchDirect(
        artist: String,
        title: String,
        album: String,
        durationSec: Int?,
    ): LrcLibResponseDto? {
        // Try without duration first — most reliable when our metadata is wrong.
        val withoutDuration = runCatching {
            lyricsApi.getLyrics(
                artistName = artist,
                trackName = title,
                durationSec = null,
                albumName = album.takeIf { it.isNotBlank() },
            )
        }.getOrNull()
        if (withoutDuration != null && !withoutDuration.syncedLyrics.isNullOrBlank()) {
            return withoutDuration
        }
        if (durationSec != null) {
            val withDuration = runCatching {
                lyricsApi.getLyrics(
                    artistName = artist,
                    trackName = title,
                    durationSec = durationSec,
                    albumName = album.takeIf { it.isNotBlank() },
                )
            }.getOrNull()
            if (withDuration != null) return withDuration
        }
        return withoutDuration
    }

    private fun hasUsableLyrics(dto: LrcLibResponseDto, syncedOnly: Boolean): Boolean {
        if (syncedOnly) {
            val synced = dto.syncedLyrics?.let(::parseLrc).orEmpty()
            return isMeaningfulSynced(synced)
        }
        return !dto.syncedLyrics.isNullOrBlank() || !dto.plainLyrics.isNullOrBlank()
    }

    private fun scoreCandidate(
        dto: LrcLibResponseDto,
        title: String,
        artist: String,
        durationSec: Int?,
        syncedOnly: Boolean,
    ): Float {
        val track = cleanTrackName(dto.trackName.orEmpty())
        val art = cleanArtistName(dto.artistName.orEmpty())
        var score = 0f

        score += titleSimilarity(title, track) * 0.55f
        score += titleSimilarity(artist, art) * 0.3f

        if (!dto.syncedLyrics.isNullOrBlank()) score += 0.15f

        val candidateDuration = dto.duration?.toInt() ?: 0
        if (durationSec != null && durationSec > 0 && candidateDuration > 0) {
            val diff = abs(durationSec - candidateDuration)
            score += when {
                diff <= 3 -> 0.12f
                diff <= 8 -> 0.05f
                diff <= 20 -> 0f
                else -> if (syncedOnly) -0.15f else -0.1f
            }
        }

        if (dto.instrumental == true) score -= 0.5f
        return score.coerceIn(0f, 1.2f)
    }

    private fun titleSimilarity(a: String, b: String): Float {
        if (a.isBlank() || b.isBlank()) return 0f
        if (a == b) return 1f
        if (a.contains(b) || b.contains(a)) return 0.85f
        val ta = a.split(Regex("\\s+")).filter { it.length > 1 }.toSet()
        val tb = b.split(Regex("\\s+")).filter { it.length > 1 }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0f
        val inter = ta.intersect(tb).size.toFloat()
        val union = ta.union(tb).size.toFloat()
        return inter / union
    }

    private fun cleanTrackName(raw: String): String {
        var s = raw.trim().lowercase()
        s = s.replace(Regex("""\([^)]*\)"""), " ")
        s = s.replace(Regex("""\[[^\]]*\]"""), " ")
        s = s.replace(
            Regex(
                """\b(official\s*(video|audio|music\s*video)?|lyrics?|hd|hq|4k|remaster(ed)?|live|visualizer|audio)\b""",
            ),
            " ",
        )
        s = s.replace(Regex("""\s+"""), " ").trim()
        return s
    }

    private fun cleanArtistName(raw: String): String {
        var s = raw.trim().lowercase()
        s = s.replace(Regex("""\s*(feat\.?|ft\.?|featuring)\s+.*$"""), "")
        s = s.replace(Regex("""\s+"""), " ").trim()
        return s
    }

    private fun toLyrics(dto: LrcLibResponseDto, syncedOnly: Boolean): Lyrics {
        val synced = dto.syncedLyrics?.let(::parseLrc).orEmpty()
        if (isMeaningfulSynced(synced)) {
            return Lyrics(lines = synced, synced = true)
        }
        if (syncedOnly) return Lyrics.EMPTY
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

    /**
     * Accepts real timed LRC (not 1–2 stub lines).
     */
    private fun isMeaningfulSynced(lines: List<LyricLine>): Boolean {
        if (lines.size < 4) return false
        val timed = lines.filter { it.timeMs >= 0 }
        if (timed.size < 4) return false
        val span = (timed.last().timeMs - timed.first().timeMs).coerceAtLeast(0L)
        return span >= 10_000L
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
