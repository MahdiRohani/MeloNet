package com.melonet.app.feature.profile

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.UserRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : BaseViewModel<ProfileContract.State, ProfileContract.Event, ProfileContract.Effect>() {

    override fun createInitialState() = ProfileContract.State()

    init {
        observePremiumStatus()
        handleEvent(ProfileContract.Event.Load)
    }

    override fun handleEvent(event: ProfileContract.Event) {
        when (event) {
            ProfileContract.Event.Load -> loadUser()
            ProfileContract.Event.UpgradePremiumClicked -> upgradePremium()
            ProfileContract.Event.EditProfileClicked -> {
                setEffect { ProfileContract.Effect.NavigateToEditProfile }
            }
            ProfileContract.Event.LikedSongsClicked -> {
                setEffect { ProfileContract.Effect.NavigateToLikedSongs }
            }
            ProfileContract.Event.MyPlaylistsClicked -> {
                setEffect { ProfileContract.Effect.NavigateToMyPlaylists }
            }
            ProfileContract.Event.FollowingClicked -> {
                setEffect { ProfileContract.Effect.NavigateToFollowing }
            }
            ProfileContract.Event.RecentlyPlayedClicked -> {
                setEffect { ProfileContract.Effect.NavigateToRecentlyPlayed }
            }
            ProfileContract.Event.LocalMusicClicked -> {
                setEffect { ProfileContract.Effect.NavigateToLocalMusic }
            }
            ProfileContract.Event.DownloadsClicked -> {
                setEffect { ProfileContract.Effect.NavigateToDownloads }
            }
        }
    }

    private fun observePremiumStatus() {
        userRepository.isPremiumFlow
            .onEach { isPremium -> setState { copy(isPremium = isPremium) } }
            .launchIn(viewModelScope)
    }

    private fun loadUser() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = userRepository.getCurrentUser()) {
                is Result.Success -> setState {
                    copy(
                        isLoading = false,
                        userName = result.data.displayName,
                        avatarUrl = result.data.avatarUrl,
                        isPremium = result.data.isPremium,
                        error = null
                    )
                }
                is Result.Error -> {
                    if (result.error is AppError.Unauthorized) {
                        setState { copy(isLoading = false) }
                    } else {
                        val message = when (val error = result.error) {
                            is AppError.Network -> error.message
                            is AppError.Unknown -> error.message
                            AppError.Unauthorized -> return@launch
                        }
                        setState { copy(isLoading = false, error = message) }
                    }
                }
            }
        }
    }

    private fun upgradePremium() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            userRepository.setPremiumStatus(true)
            setState { copy(isLoading = false, isPremium = true) }
        }
    }
}
