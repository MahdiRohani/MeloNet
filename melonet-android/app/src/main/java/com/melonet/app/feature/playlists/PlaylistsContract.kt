package com.melonet.app.feature.playlists

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Playlist

object PlaylistsContract {

    data class State(
        val isLoading: Boolean = true,
        val systemPlaylists: List<Playlist> = emptyList(),
        val userPlaylists: List<Playlist> = emptyList(),
        val error: AppError? = null,
        val showCreateDialog: Boolean = false,
        val createTitle: String = "",
        val isCreating: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object Load : Event
        data object Refresh : Event
        data class PlaylistClicked(val playlist: Playlist) : Event
        data object ShowCreateDialog : Event
        data object HideCreateDialog : Event
        data class CreateTitleChanged(val title: String) : Event
        data object CreatePlaylist : Event
        data class DeletePlaylist(val playlist: Playlist) : Event
        data object NavigateToLiked : Event
        data object NavigateToRecent : Event
    }

    sealed interface Effect : UiEffect {
        data class NavigateToDetail(val playlistId: Int) : Effect
        data object NavigateToLiked : Effect
        data object NavigateToRecent : Effect
        data class ShowError(val error: AppError) : Effect
    }
}
