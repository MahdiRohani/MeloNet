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

            if (candidates.isEmpty()) {
                setState { copy(suggestions = emptyList(), isLoadingSuggestions = false) }
                return@launch
            }

            // Show catalog songs immediately so the hub is never empty.
            setState {
                copy(
                    suggestions = candidates.take(20),
                    isLoadingSuggestions = false,
                )
            }

            // Prefer tracks that actually have timed LRC; reorder when probe finishes.
            val synced = filterSynced(candidates).take(16)
            if (synced.isNotEmpty()) {
                setState {
                    copy(
                        suggestions = (synced + candidates.filter { c -> synced.none { it.id == c.id } })
                            .distinctBy { it.id }
                            .take(20),
                    )
                }
            }
        }
    }

    private suspend fun filterSynced(songs: List<Song>): List<Song> = coroutineScope {
        val embedded = songs.filter { lyricsRepository.hasEmbeddedSyncedLyrics(it.lyrics) }
        val remaining = songs.filter { song -> embedded.none { it.id == song.id } }.take(12)
        val probed = remaining.map { song ->
            async {
                val lyrics = runCatching {
                    lyricsRepository.getLyrics(
                        title = song.title,
                        artist = song.artistName,
                        durationSec = song.durationSec,
                        album = song.albumTitle,
                        embeddedLyrics = song.lyrics.takeIf { it.isNotBlank() },
                        syncedOnly = true,
                    )
                }.getOrNull()
                song.takeIf { lyrics != null && lyrics.synced && lyrics.lines.isNotEmpty() }
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
            setState { copy(results = songs, isSearching = false, hasSearched = true) }
        }
    }
}
