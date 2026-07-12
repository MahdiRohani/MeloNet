package com.melonet.app.feature.social

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Playlist
import com.melonet.app.data.model.PublicUser

object UserProfileContract {
    data class State(
        val isLoading: Boolean = true,
        val user: PublicUser? = null,
        val playlists: List<Playlist> = emptyList(),
        val isFollowLoading: Boolean = false,
        val error: AppError? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val userId: Int) : Event
        data object ToggleFollow : Event
        data object FollowersClicked : Event
        data object FollowingClicked : Event
        data class PlaylistClicked(val playlistId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class NavigateToFollowers(val userId: Int) : Effect
        data class NavigateToFollowing(val userId: Int) : Effect
        data class NavigateToPlaylist(val playlistId: Int) : Effect
    }
}
