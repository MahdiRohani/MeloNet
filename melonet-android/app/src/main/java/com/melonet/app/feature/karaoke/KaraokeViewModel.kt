package com.melonet.app.feature.karaoke

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KaraokeViewModel(
    private val searchRepository: SearchRepository,
) : BaseViewModel<KaraokeContract.State, KaraokeContract.Event, KaraokeContract.Effect>() {

    private var searchJob: Job? = null

    override fun createInitialState() = KaraokeContract.State()

    override fun handleEvent(event: KaraokeContract.Event) {
        when (event) {
            is KaraokeContract.Event.QueryChanged -> {
                setState { copy(query = event.query) }
                scheduleSearch(event.query)
            }
            KaraokeContract.Event.Submit -> runSearch(uiState.value.query, immediate = true)
        }
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            setState { copy(results = emptyList(), isSearching = false, hasSearched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            runSearch(query, immediate = false)
        }
    }

    private fun runSearch(query: String, immediate: Boolean) {
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
