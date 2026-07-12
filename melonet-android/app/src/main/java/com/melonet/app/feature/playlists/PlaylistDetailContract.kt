package com.melonet.app.feature.playlists

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Playlist

object PlaylistDetailContract {

    data class State(
        val isLoading: Boolean = true,
        val playlist: Playlist? = null,
        val error: AppError? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val playlistId: Int) : Event
        data class SongClicked(val songId: Int) : Event
        data object PlayAll : Event
        data object ShuffleAll : Event
    }

    sealed interface Effect : UiEffect {
        data class NavigateToPlayer(val songId: Int) : Effect
        data class PlayQueue(val startSongId: Int, val shuffle: Boolean) : Effect
        data class ShowError(val error: AppError) : Effect
    }
}
