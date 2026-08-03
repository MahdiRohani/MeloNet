package com.melonet.app.feature.voicecover

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.VoiceCover

object VoiceCoverPlayerContract {

    data class State(
        val cover: VoiceCover? = null,
        val isLoading: Boolean = true,
        val isPolling: Boolean = false,
        val error: String? = null,
        val autoPlayed: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val coverId: Long) : Event
        data object Play : Event
        data object Retry : Event
        data object Delete : Event
    }

    sealed interface Effect : UiEffect {
        data class PlayCover(val cover: VoiceCover) : Effect
        data object Deleted : Effect
    }
}
