package com.melonet.app.feature.search

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.SearchFilter
import com.melonet.app.data.model.SearchResultItem
import com.melonet.app.data.repository.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchRepository: SearchRepository,
) : BaseViewModel<SearchContract.State, SearchContract.Event, SearchContract.Effect>() {

    private val queryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow(SearchFilter.ALL)

    val searchResults: Flow<PagingData<SearchResultItem>> = combine(
        queryFlow.debounce(DEBOUNCE_MS).distinctUntilChanged(),
        filterFlow,
    ) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        if (query.isBlank()) {
            flowOf(PagingData.empty())
        } else {
            searchRepository.searchResults(query, filter)
        }
    }.cachedIn(viewModelScope)

    override fun createInitialState() = SearchContract.State()

    init {
        observeHistory()
    }

    override fun handleEvent(event: SearchContract.Event) {
        when (event) {
            is SearchContract.Event.QueryChanged -> {
                val trimmed = event.query.trim()
                setState { copy(query = event.query, isSearching = trimmed.isNotBlank()) }
                queryFlow.value = trimmed
            }
            is SearchContract.Event.QuerySubmitted -> {
                val trimmed = event.query.trim()
                if (trimmed.isNotBlank()) {
                    setState { copy(query = trimmed, isSearching = true) }
                    queryFlow.value = trimmed
                    viewModelScope.launch { searchRepository.saveHistory(trimmed) }
                }
            }
            is SearchContract.Event.FilterSelected -> {
                filterFlow.value = event.filter
                setState { copy(selectedFilter = event.filter) }
            }
            is SearchContract.Event.HistoryItemClicked -> {
                setState { copy(query = event.query, isSearching = true) }
                queryFlow.value = event.query
            }
            is SearchContract.Event.HistoryItemDeleted -> {
                viewModelScope.launch {
                    searchRepository.deleteHistory(event.query)
                }
            }
            is SearchContract.Event.ResultClicked -> {
                when (val item = event.item) {
                    is SearchResultItem.SongItem -> {
                        viewModelScope.launch { searchRepository.saveHistory(uiState.value.query.trim()) }
                        setEffect { SearchContract.Effect.PlaySong(item.song) }
                    }
                    is SearchResultItem.ArtistItem -> {
                        viewModelScope.launch { searchRepository.saveHistory(uiState.value.query.trim()) }
                        setEffect { SearchContract.Effect.NavigateToArtist(item.artist.id) }
                    }
                    is SearchResultItem.UserItem -> {
                        viewModelScope.launch { searchRepository.saveHistory(uiState.value.query.trim()) }
                        setEffect { SearchContract.Effect.NavigateToUser(item.user.id) }
                    }
                }
            }
        }
    }

    private fun observeHistory() {
        searchRepository.observeHistory()
            .onEach { history -> setState { copy(history = history) } }
            .launchIn(viewModelScope)
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
    }
}
