package com.melonet.app.feature.catalog

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState

object CatalogContract {

    data class State(
        val listType: String = "",
        val filter: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class SongClicked(val songId: String) : Event
        data object PlayAll : Event
        data object ShuffleAll : Event
    }

    sealed interface Effect : UiEffect {
        data class PlayQueue(val startSongId: String, val shuffle: Boolean) : Effect
    }
}
