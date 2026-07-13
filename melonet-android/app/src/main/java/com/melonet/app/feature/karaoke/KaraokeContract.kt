package com.melonet.app.feature.karaoke

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Song

object KaraokeContract {

    data class State(
        val query: String = "",
        val results: List<Song> = emptyList(),
        val isSearching: Boolean = false,
        val hasSearched: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data object Submit : Event
    }

    sealed interface Effect : UiEffect
}
