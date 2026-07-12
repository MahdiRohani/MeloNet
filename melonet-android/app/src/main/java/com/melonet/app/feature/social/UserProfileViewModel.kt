package com.melonet.app.feature.social

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.SocialRepository
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val socialRepository: SocialRepository,
) : BaseViewModel<UserProfileContract.State, UserProfileContract.Event, UserProfileContract.Effect>() {

    private var userId: Int = 0

    override fun createInitialState() = UserProfileContract.State()

    override fun handleEvent(event: UserProfileContract.Event) {
        when (event) {
            is UserProfileContract.Event.Load -> load(event.userId)
            UserProfileContract.Event.ToggleFollow -> toggleFollow()
            UserProfileContract.Event.FollowersClicked -> {
                if (userId > 0) setEffect { UserProfileContract.Effect.NavigateToFollowers(userId) }
            }
            UserProfileContract.Event.FollowingClicked -> {
                if (userId > 0) setEffect { UserProfileContract.Effect.NavigateToFollowing(userId) }
            }
            is UserProfileContract.Event.PlaylistClicked -> {
                setEffect { UserProfileContract.Effect.NavigateToPlaylist(event.playlistId) }
            }
        }
    }

    private fun load(id: Int) {
        userId = id
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val profileResult = socialRepository.getUserProfile(id)
            val playlistsResult = socialRepository.getUserPlaylists(id)
            when (profileResult) {
                is Result.Success -> setState {
                    copy(
                        isLoading = false,
                        user = profileResult.data,
                        playlists = (playlistsResult as? Result.Success)?.data.orEmpty(),
                        error = null,
                    )
                }
                is Result.Error -> setState { copy(isLoading = false, error = profileResult.error) }
            }
        }
    }

    private fun toggleFollow() {
        val user = uiState.value.user ?: return
        if (user.isSelf) return
        viewModelScope.launch {
            setState { copy(isFollowLoading = true) }
            val result = if (user.isFollowing) {
                socialRepository.unfollow(user.id)
            } else {
                socialRepository.follow(user.id)
            }
            when (result) {
                is Result.Success -> setState { copy(isFollowLoading = false, user = result.data) }
                is Result.Error -> setState { copy(isFollowLoading = false, error = result.error) }
            }
        }
    }
}
