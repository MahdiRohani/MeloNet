package com.melonet.app.feature.profile

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiState
import com.melonet.app.core.common.UiEvent

object ProfileContract {

    data class State(
        val userName: String = "",
        val avatarUrl: String = "",
        val isPremium: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : UiState

    sealed interface Event : UiEvent {
        data object Load : Event
        data object UpgradePremiumClicked : Event
        data object EditProfileClicked : Event
        data object LikedSongsClicked : Event
        data object MyPlaylistsClicked : Event
        data object FollowingClicked : Event
        data object RecentlyPlayedClicked : Event
        data object LocalMusicClicked : Event
        data object DownloadsClicked : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowError(val message: String) : Effect
        data object NavigateToEditProfile : Effect
        data object NavigateToLikedSongs : Effect
        data object NavigateToMyPlaylists : Effect
        data object NavigateToFollowing : Effect
        data object NavigateToRecentlyPlayed : Effect
        data object NavigateToLocalMusic : Effect
        data object NavigateToDownloads : Effect
    }
}
