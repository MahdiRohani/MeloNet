package com.melonet.app.feature.artist

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Artist
import com.melonet.app.data.model.SortOption

object ArtistDetailContract {

    data class State(
        val artist: Artist? = null,
        val isLoading: Boolean = true,
        val isFollowLoading: Boolean = false,
        val sort: SortOption = SortOption.MOST_PLAYED,
        val error: AppError? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val artistId: Int) : Event
        data object ToggleFollow : Event
        data class SortSelected(val sort: SortOption) : Event
        data class SongClicked(val songId: String) : Event
    }

    sealed interface Effect : UiEffect {
        data class PlayQueue(val startSongId: String) : Effect
        data class ShowError(val error: AppError) : Effect
    }
}
