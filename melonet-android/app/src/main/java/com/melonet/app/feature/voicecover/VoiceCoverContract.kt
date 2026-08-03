package com.melonet.app.feature.voicecover

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Song
import com.melonet.app.data.model.VoiceCover

object VoiceCoverContract {

    data class State(
        val query: String = "",
        val results: List<Song> = emptyList(),
        val readyCovers: List<VoiceCover> = emptyList(),
        val isLoadingCatalog: Boolean = true,
        val isSearching: Boolean = false,
        val hasSearched: Boolean = false,
        val searchError: String? = null,
        val catalogError: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data object Submit : Event
        data object Refresh : Event
        data class CoverClicked(val cover: VoiceCover) : Event
        data class CoverDelete(val cover: VoiceCover) : Event
        data class SongClicked(val song: Song) : Event
    }

    sealed interface Effect : UiEffect {
        data class OpenCreate(val songId: String) : Effect
        data class OpenPlayer(val coverId: Long) : Effect
    }
}
