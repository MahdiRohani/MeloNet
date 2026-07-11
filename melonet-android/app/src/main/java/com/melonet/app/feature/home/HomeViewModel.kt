package com.melonet.app.feature.home

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.HomeRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository
) : BaseViewModel<HomeContract.State, HomeContract.Event, HomeContract.Effect>() {

    override fun createInitialState() = HomeContract.State()

    init {
        handleEvent(HomeContract.Event.Load)
    }

    override fun handleEvent(event: HomeContract.Event) {
        when (event) {
            HomeContract.Event.Load,
            HomeContract.Event.Refresh -> loadHomeFeed()
            is HomeContract.Event.SongClicked -> {
                setEffect { HomeContract.Effect.NavigateToPlayer(event.songId) }
            }
            is HomeContract.Event.QuickActionClicked -> {
                // Navigation handled in a later phase
            }
        }
    }

    private fun loadHomeFeed() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = homeRepository.getHomeFeed()) {
                is Result.Success -> setState {
                    copy(isLoading = false, feed = result.data, error = null)
                }
                is Result.Error -> {
                    val message = when (val error = result.error) {
                        is AppError.Network -> error.message
                        is AppError.Unauthorized -> "لطفاً وارد شوید"
                        is AppError.Unknown -> error.message
                    }
                    setState { copy(isLoading = false, error = message) }
                    setEffect { HomeContract.Effect.ShowError(message) }
                }
            }
        }
    }
}
