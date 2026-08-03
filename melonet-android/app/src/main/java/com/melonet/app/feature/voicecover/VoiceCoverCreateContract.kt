package com.melonet.app.feature.voicecover

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Song
import com.melonet.app.data.model.VoiceArtist
import com.melonet.app.data.model.VoiceCover

object VoiceCoverCreateContract {

    data class State(
        val song: Song? = null,
        val artists: List<VoiceArtist> = emptyList(),
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false,
        val selectedSlug: String? = null,
        val activeCover: VoiceCover? = null,
        val error: String? = null,
    ) : UiState {
        val isProcessing: Boolean
            get() = activeCover?.isInProgress == true
    }

    sealed interface Event : UiEvent {
        data class Load(val songId: String) : Event
        data class ArtistSelected(val slug: String) : Event
        data object Retry : Event
    }

    sealed interface Effect : UiEffect {
        data class OpenPlayer(val coverId: Long) : Effect
    }
}
