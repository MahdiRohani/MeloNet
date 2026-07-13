package com.melonet.app.feature.catalog

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.SortOption

object CatalogContract {

    data class State(
        val listType: String = "",
        val filter: String? = null,
        val sort: SortOption = SortOption.MOST_PLAYED,
    ) : UiState

    sealed interface Event : UiEvent {
        data class SongClicked(val songId: String) : Event
        data class SortSelected(val sort: SortOption) : Event
    }

    sealed interface Effect : UiEffect {
        data class PlayQueue(val startSongId: String, val shuffle: Boolean) : Effect
    }
}
