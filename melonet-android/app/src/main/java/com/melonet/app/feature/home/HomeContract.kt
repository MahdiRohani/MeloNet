package com.melonet.app.feature.home

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Artist
import com.melonet.app.data.model.HomeFeed
import com.melonet.app.data.model.HomeRow
import com.melonet.app.data.model.QuickAction
import com.melonet.app.data.model.Song

object HomeContract {

    data class State(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val feed: HomeFeed? = null,
        val error: AppError? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object Load : Event
        data object Refresh : Event
        data class SongClicked(val song: Song) : Event
        data class QuickActionClicked(val action: QuickAction) : Event
        data class SeeAllClicked(val row: HomeRow) : Event
        data class ArtistClicked(val artist: Artist) : Event
    }

    sealed interface Effect : UiEffect {
        data class PlaySong(val song: Song, val queue: List<Song> = emptyList()) : Effect
        data class Navigate(val destination: HomeDestination) : Effect
        data class ShowError(val error: AppError) : Effect
    }
}
