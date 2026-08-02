package com.melonet.app.feature.karaoke

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.HomeRepository
import com.melonet.app.data.repository.LyricsRepository
import com.melonet.app.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicReference

class KaraokeViewModel(
    private val searchRepository: SearchRepository,
    private val homeRepository: HomeRepository,
    private val lyricsRepository: LyricsRepository,
) : BaseViewModel<KaraokeContract.State, KaraokeContract.Event, KaraokeContract.Effect>() {

    private var searchJob: Job? = null

    override fun createInitialState() = KaraokeContract.State()

    init {
        loadSuggestions()
    }

    override fun handleEvent(event: KaraokeContract.Event) {
        when (event) {
            is KaraokeContract.Event.QueryChanged -> {
                setState { copy(query = event.query, searchError = null) }
                scheduleSearch(event.query)
            }
            KaraokeContract.Event.Submit -> runSearch(uiState.value.query)
            KaraokeContract.Event.RefreshSuggestions -> loadSuggestions(force = true)
        }
    }

    private fun loadSuggestions(force: Boolean = false) {
        viewModelScope.launch {
            if (!force) {
                suggestionCache.get()?.takeIf { it.isNotEmpty() }?.let { cached ->
                    setState {
                        copy(
                            suggestions = cached,
                            isLoadingSuggestions = false,
                        )
                    }
                    // Refresh quietly in background.
                }
            }

            setState {
                copy(
                    isLoadingSuggestions = suggestions.isEmpty(),
                )
            }

            val found = mutableListOf<Song>()
            val foundLock = Mutex()

            suspend fun publish(song: Song) {
                foundLock.withLock {
                    if (found.any { it.id == song.id }) return
                    found += song
                    val snapshot = found.toList()
                    suggestionCache.set(snapshot)
                    setState {
                        copy(
                            suggestions = snapshot,
                            isLoadingSuggestions = snapshot.size < TARGET_COUNT,
                        )
                    }
                }
            }

            // 1) Fast path: probe already-warm home feed (few LRCLIB calls).
            val homeCandidates = homeRepository.peekCachedFeed()?.let { feed ->
                (feed.carousel + feed.rows.flatMap { it.items }).distinctBy { it.id }
            }.orEmpty().ifEmpty {
                when (val result = homeRepository.getHomeFeed()) {
                    is Result.Success -> {
                        val feed = result.data
                        (feed.carousel + feed.rows.flatMap { it.items }).distinctBy { it.id }
                    }
                    is Result.Error -> emptyList()
                }
            }

            probeUntilFilled(
                songs = homeCandidates.take(16),
                target = TARGET_COUNT,
                onHit = { publish(it) },
                shouldStop = { foundLock.withLock { found.size >= TARGET_COUNT } },
            )

            // 2) Top up with a small curated search set if still short.
            if (found.size < TARGET_COUNT) {
                val curated = collectCuratedCandidates(limitPerQuery = 1)
                probeUntilFilled(
                    songs = curated,
                    target = TARGET_COUNT,
                    onHit = { publish(it) },
                    shouldStop = { foundLock.withLock { found.size >= TARGET_COUNT } },
                )
            }

            setState {
                copy(
                    suggestions = found.toList().ifEmpty { suggestions },
                    isLoadingSuggestions = false,
                )
            }
            if (found.isNotEmpty()) {
                suggestionCache.set(found.toList())
            }
        }
    }

    private suspend fun collectCuratedCandidates(limitPerQuery: Int): List<Song> = coroutineScope {
        val semaphore = Semaphore(4)
        SUGGESTION_QUERIES.map { query ->
            async {
                semaphore.withPermit {
                    runCatching {
                        searchRepository.searchSongs(query, limit = limitPerQuery)
                    }.getOrElse { emptyList() }
                }
            }
        }.awaitAll()
            .flatten()
            .distinctBy { it.id }
    }

    private suspend fun probeUntilFilled(
        songs: List<Song>,
        target: Int,
        onHit: suspend (Song) -> Unit,
        shouldStop: suspend () -> Boolean,
    ) = coroutineScope {
        if (songs.isEmpty()) return@coroutineScope
        val semaphore = Semaphore(6)
        songs.map { song ->
            async {
                if (shouldStop()) return@async
                semaphore.withPermit {
                    if (shouldStop()) return@withPermit
                    val ok = if (lyricsRepository.hasEmbeddedSyncedLyrics(song.lyrics)) {
                        true
                    } else {
                        runCatching {
                            lyricsRepository.hasSyncedLyricsFast(song.title, song.artistName)
                        }.getOrDefault(false)
                    }
                    if (ok) onHit(song)
                }
            }
        }.awaitAll()
        // silence unused target warning in signature for readability
        @Suppress("UNUSED_EXPRESSION")
        target
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            setState {
                copy(
                    results = emptyList(),
                    isSearching = false,
                    hasSearched = false,
                    searchError = null,
                )
            }
            return
        }
        if (trimmed.length < 2) {
            setState {
                copy(
                    results = emptyList(),
                    isSearching = false,
                    hasSearched = false,
                    searchError = null,
                )
            }
            return
        }
        setState { copy(isSearching = true, hasSearched = false, searchError = null) }
        searchJob = viewModelScope.launch {
            delay(450)
            runSearch(trimmed)
        }
    }

    private fun runSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            setState { copy(isSearching = true, searchError = null) }
            when (val result = searchRepository.searchSongsResult(trimmed, limit = 30)) {
                is Result.Success -> {
                    setState {
                        copy(
                            results = result.data,
                            isSearching = false,
                            hasSearched = true,
                            searchError = null,
                        )
                    }
                }
                is Result.Error -> {
                    setState {
                        copy(
                            results = emptyList(),
                            isSearching = false,
                            hasSearched = true,
                            searchError = mapSearchError(result.error),
                        )
                    }
                }
            }
        }
    }

    private fun mapSearchError(error: AppError): String = when (error) {
        is AppError.Http -> when (error.code) {
            "rate_limited" -> "rate_limited"
            else -> error.code
        }
        AppError.NoConnection -> "no_connection"
        AppError.Timeout -> "timeout"
        else -> "search_failed"
    }

    companion object {
        private const val TARGET_COUNT = 10
        private val suggestionCache = AtomicReference<List<Song>?>(null)

        private val SUGGESTION_QUERIES = listOf(
            "Mohsen Yeganeh Behet Ghol Midam",
            "Coldplay Yellow",
            "Ed Sheeran Perfect",
            "Adele Someone Like You",
            "Imagine Dragons Believer",
            "The Weeknd Blinding Lights",
        )
    }
}
