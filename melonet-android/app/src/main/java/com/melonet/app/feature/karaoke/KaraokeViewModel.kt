package com.melonet.app.feature.karaoke

import androidx.lifecycle.viewModelScope
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
                setState { copy(query = event.query) }
                scheduleSearch(event.query)
            }
            KaraokeContract.Event.Submit -> runSearch(uiState.value.query)
            KaraokeContract.Event.RefreshSuggestions -> loadSuggestions()
        }
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            setState { copy(isLoadingSuggestions = true) }
            val candidates = when (val result = homeRepository.getHomeFeed()) {
                is Result.Success -> {
                    val feed = result.data
                    (feed.carousel + feed.rows.flatMap { it.items })
                        .distinctBy { it.id }
                }
                is Result.Error -> emptyList()
            }
            val synced = filterSynced(candidates).take(12)
            setState { copy(suggestions = synced, isLoadingSuggestions = false) }
        }
    }

    private suspend fun filterSynced(songs: List<Song>): List<Song> = coroutineScope {
        // Prefer songs that already ship real LRC embedded in the catalog.
        val embedded = songs.filter { lyricsRepository.hasEmbeddedSyncedLyrics(it.lyrics) }
        if (embedded.size >= 6) return@coroutineScope embedded

        // Probe a limited batch via LRCLIB so hub stays synced-only without flooding the API.
        val remaining = songs.filter { song -> embedded.none { it.id == song.id } }.take(16)
        val probed = remaining.map { song ->
            async {
                val lyrics = lyricsRepository.getLyrics(
                    title = song.title,
                    artist = song.artistName,
                    durationSec = song.durationSec,
                    album = song.albumTitle,
                    embeddedLyrics = song.lyrics.takeIf { it.isNotBlank() },
                    syncedOnly = true,
                )
                song.takeIf { lyrics.synced && lyrics.lines.isNotEmpty() }
            }
        }.awaitAll().filterNotNull()

        (embedded + probed).distinctBy { it.id }
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            setState { copy(results = emptyList(), isSearching = false, hasSearched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            runSearch(query)
        }
    }

    private fun runSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            setState { copy(isSearching = true) }
            val songs = searchRepository.searchSongs(trimmed, limit = 30)
            // Keep search snappy: surface catalog hits; player enforces synced-only LRC.
            setState { copy(results = songs, isSearching = false, hasSearched = true) }
        }
    }
}
