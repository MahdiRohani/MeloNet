package com.melonet.app.feature.following

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.UserListType
import com.melonet.app.data.repository.ArtistRepository
import com.melonet.app.data.repository.SearchRepository
import com.melonet.app.data.repository.SocialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FollowingViewModel(
    private val socialRepository: SocialRepository,
    private val artistRepository: ArtistRepository,
    private val searchRepository: SearchRepository,
) : BaseViewModel<FollowingContract.State, FollowingContract.Event, FollowingContract.Effect>() {

    private var searchJob: Job? = null

    override fun createInitialState() = FollowingContract.State()

    override fun handleEvent(event: FollowingContract.Event) {
        when (event) {
            is FollowingContract.Event.Load -> load(event.userId)
            is FollowingContract.Event.QueryChanged -> {
                setState { copy(query = event.query) }
                scheduleUserDiscovery(event.query)
            }
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

    private fun scheduleUserDiscovery(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            setState { copy(searchResults = emptyList(), isSearchingUsers = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            setState { copy(isSearchingUsers = true) }
            val users = searchRepository.searchUsers(trimmed, limit = 20)
            // Exclude people already in following list from "discover" section.
            val followingIds = uiState.value.users.map { it.id }.toSet()
            setState {
                copy(
                    searchResults = users.filterNot { it.id in followingIds },
                    isSearchingUsers = false,
                )
            }
        }
    }
}
