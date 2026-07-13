package com.melonet.app.feature.following

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.UserListType
import com.melonet.app.data.repository.ArtistRepository
import com.melonet.app.data.repository.SocialRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FollowingViewModel(
    private val socialRepository: SocialRepository,
    private val artistRepository: ArtistRepository,
) : BaseViewModel<FollowingContract.State, FollowingContract.Event, FollowingContract.Effect>() {

    override fun createInitialState() = FollowingContract.State()

    override fun handleEvent(event: FollowingContract.Event) {
        when (event) {
            is FollowingContract.Event.Load -> load(event.userId)
        }
    }

    private fun load(userId: Int) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val usersDeferred = async { socialRepository.getUserList(userId, UserListType.FOLLOWING) }
            val artistsDeferred = async { artistRepository.followedArtists() }

            val usersResult = usersDeferred.await()
            val artistsResult = artistsDeferred.await()

            setState {
                copy(
                    isLoading = false,
                    users = (usersResult as? Result.Success)?.data ?: users,
                    artists = (artistsResult as? Result.Success)?.data ?: artists,
                    error = (usersResult as? Result.Error)?.error,
                )
            }
        }
    }
}
