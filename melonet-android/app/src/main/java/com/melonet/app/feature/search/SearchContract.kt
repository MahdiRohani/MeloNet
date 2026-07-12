package com.melonet.app.feature.search

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.SearchFilter
import com.melonet.app.data.model.SearchResultItem

object SearchContract {

    data class State(
        val query: String = "",
        val selectedFilter: SearchFilter = SearchFilter.ALL,
        val history: List<String> = emptyList(),
        val isSearching: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data class FilterSelected(val filter: SearchFilter) : Event
        data class HistoryItemClicked(val query: String) : Event
        data class HistoryItemDeleted(val query: String) : Event
        data class ResultClicked(val item: SearchResultItem) : Event
    }

    sealed interface Effect : UiEffect {
        data class NavigateToSong(val songId: Int) : Effect
        data class NavigateToArtist(val artistId: Int) : Effect
        data class NavigateToUser(val userId: Int) : Effect
    }
}
