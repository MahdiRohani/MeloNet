package com.melonet.app.feature.home

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.domain.model.HomeFeed
import com.melonet.app.domain.model.QuickAction

object HomeContract {

    data class State(
        val isLoading: Boolean = true,
        val feed: HomeFeed? = null,
        val error: String? = null
    ) : UiState

    sealed interface Event : UiEvent {
        data object Load : Event
        data object Refresh : Event
        data class SongClicked(val songId: Int) : Event
        data class QuickActionClicked(val action: QuickAction) : Event
    }

    sealed interface Effect : UiEffect {
        data class NavigateToPlayer(val songId: Int) : Effect
        data class ShowError(val message: String) : Effect
    }
}
