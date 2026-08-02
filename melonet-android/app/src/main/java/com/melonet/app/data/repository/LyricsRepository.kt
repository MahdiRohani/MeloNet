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
        embeddedLyrics?.takeIf { it.isNotBlank() }?.let { raw ->
            val parsed = parseLrc(raw)
            if (isMeaningfulSynced(parsed)) {
                return@withContext Lyrics(parsed, synced = true)
            }
            if (!syncedOnly && parsed.isNotEmpty()) {
                return@withContext Lyrics(parsed, synced = parsed.any { it.timeMs >= 0 })
            }
        }

        val (resolvedArtist, resolvedTitle) = resolveArtistTitle(
            cleanArtistName(artist),
            cleanTrackName(title),
        )
        val reliableDuration = durationSec.takeIf { it in 30..900 }

        // Search-first: LRCLIB /api/get is brittle with Audius metadata.
        val searchHits = collectSearchHits(resolvedTitle, resolvedArtist, title, artist)
        val fromSearch = pickBest(searchHits, resolvedTitle, resolvedArtist, reliableDuration, syncedOnly)
        if (fromSearch != null) return@withContext fromSearch

        val direct = fetchDirect(
            artist = resolvedArtist,
            title = resolvedTitle,
            album = album?.let(::cleanTrackName).orEmpty(),
            durationSec = reliableDuration,
        )
        if (direct != null) {
            val lyrics = toLyrics(direct, syncedOnly)
            if (lyrics.lines.isNotEmpty()) return@withContext lyrics
        }

        Lyrics.EMPTY
    }

    private suspend fun collectSearchHits(
        resolvedTitle: String,
        resolvedArtist: String,
        rawTitle: String,
        rawArtist: String,
    ): List<LrcLibResponseDto> {
        val queries = linkedSetOf<String>()
        if (resolvedTitle.isNotBlank() && resolvedArtist.isNotBlank()) {
            queries += "$resolvedTitle $resolvedArtist"
            queries += "$resolvedArtist $resolvedTitle"
        }
        if (resolvedTitle.isNotBlank()) queries += resolvedTitle
        if (rawTitle.isNotBlank()) queries += rawTitle.trim()
        if (rawArtist.isNotBlank() && resolvedTitle.isNotBlank()) {
            queries += "$rawTitle $rawArtist"
        }

        val hits = ArrayList<LrcLibResponseDto>()
        for (q in queries.take(4)) {
            val batch = runCatching { lyricsApi.searchLyrics(q) }.getOrNull().orEmpty()
            hits += batch
            // Early exit when we already have usable synced lyrics.
            if (batch.any { !it.syncedLyrics.isNullOrBlank() }) break
        }
        return hits
    }

    private fun pickBest(
        hits: List<LrcLibResponseDto>,
        title: String,
        artist: String,
        durationSec: Int?,
        syncedOnly: Boolean,
    ): Lyrics? {
        if (hits.isEmpty()) return null
        val ranked = hits
            .asSequence()
            .filter { hasUsableLyrics(it, syncedOnly) }
            .map { it to scoreCandidate(it, title, artist, durationSec, syncedOnly) }
            .sortedByDescending { it.second }
            .toList()
        // Accept even weak title matches if synced lyrics exist — karaoke needs *some* text.
        val best = ranked.firstOrNull { it.second >= 0.15f }?.first
            ?: ranked.firstOrNull()?.first
            ?: return null
        val lyrics = toLyrics(best, syncedOnly)
        return lyrics.takeIf { it.lines.isNotEmpty() }
    }

    private fun resolveArtistTitle(artist: String, title: String): Pair<String, String> {
        val seps = listOf(" - ", " – ", " — ", " | ")
        for (sep in seps) {
            val idx = title.indexOf(sep)
            if (idx < 2) continue
            val a = title.substring(0, idx).trim()
            val t = title.substring(idx + sep.length).trim()
            if (a.length >= 2 && t.isNotEmpty()) {
                return a to t
            }
        }
        return artist to title
    }

    fun hasEmbeddedSyncedLyrics(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return isMeaningfulSynced(parseLrc(raw))
    }

    /** One LRCLIB search — for suggestion probing only (not full player fetch). */
    suspend fun hasSyncedLyricsFast(
        title: String,
        artist: String,
    ): Boolean = withContext(dispatchers.io) {
        val (resolvedArtist, resolvedTitle) = resolveArtistTitle(
            cleanArtistName(artist),
            cleanTrackName(title),
        )
        val query = when {
            resolvedTitle.isNotBlank() && resolvedArtist.isNotBlank() ->
                "$resolvedArtist $resolvedTitle"
            resolvedTitle.isNotBlank() -> resolvedTitle
            else -> title.trim()
        }
        if (query.isBlank()) return@withContext false
        val hits = runCatching { lyricsApi.searchLyrics(query) }.getOrNull().orEmpty()
        hits.any { !it.syncedLyrics.isNullOrBlank() && isMeaningfulSynced(parseLrc(it.syncedLyrics!!)) }
    }

    private suspend fun fetchDirect(
        artist: String,
        title: String,
        album: String,
        durationSec: Int?,
    ): LrcLibResponseDto? {
        if (artist.isBlank() || title.isBlank()) return null
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
            return isMeaningfulSynced(dto.syncedLyrics?.let(::parseLrc).orEmpty())
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
        if (!dto.syncedLyrics.isNullOrBlank()) score += 0.2f

        val candidateDuration = dto.duration?.toInt() ?: 0
        if (durationSec != null && durationSec > 0 && candidateDuration > 0) {
            val diff = abs(durationSec - candidateDuration)
            score += when {
                diff <= 5 -> 0.1f
                diff <= 15 -> 0.03f
                else -> -0.05f
            }
        }
        if (dto.instrumental == true) score -= 0.5f
        return score.coerceIn(0f, 1.3f)
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
        if (syncedOnly) {
            // Still show short synced blobs rather than nothing.
            if (synced.size >= 2) return Lyrics(lines = synced, synced = true)
            return Lyrics.EMPTY
        }
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

    private fun isMeaningfulSynced(lines: List<LyricLine>): Boolean {
        val timed = lines.filter { it.timeMs >= 0 }
        if (timed.size < 2) return false
        val span = (timed.last().timeMs - timed.first().timeMs).coerceAtLeast(0L)
        return timed.size >= 4 || span >= 8_000L
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
