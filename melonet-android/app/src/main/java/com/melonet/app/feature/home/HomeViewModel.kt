package com.melonet.app.feature.home

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.HomeRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepository: HomeRepository,
) : BaseViewModel<HomeContract.State, HomeContract.Event, HomeContract.Effect>() {

    override fun createInitialState() = HomeContract.State()

    init {
        handleEvent(HomeContract.Event.Load)
    }

    override fun handleEvent(event: HomeContract.Event) {
        when (event) {
            HomeContract.Event.Load -> loadHomeFeed(refreshing = false)
            HomeContract.Event.Refresh -> loadHomeFeed(refreshing = true)
            is HomeContract.Event.SongClicked -> {
                val queue = buildSongQueue(event.song)
                setEffect { HomeContract.Effect.PlaySong(event.song, queue) }
            }
            is HomeContract.Event.QuickActionClicked -> {
                navigateFromTarget(event.action.target)
            }
            is HomeContract.Event.SeeAllClicked -> {
                event.row.seeAllPath?.let { path ->
                    HomeNavigation.parseSeeAllPath(path)?.let { destination ->
                        setEffect { HomeContract.Effect.Navigate(destination) }
                    }
                }
            }
        }
    }

    private fun buildSongQueue(startSong: com.melonet.app.data.model.Song): List<com.melonet.app.data.model.Song> {
        val feed = uiState.value.feed ?: return listOf(startSong)
        val allSongs = buildList {
            addAll(feed.carousel)
            feed.rows.forEach { row -> addAll(row.items) }
        }.distinctBy { it.id }
        if (allSongs.isEmpty()) return listOf(startSong)
        val index = allSongs.indexOfFirst { it.id == startSong.id }.coerceAtLeast(0)
        return allSongs.drop(index).ifEmpty { listOf(startSong) }
    }

    private fun navigateFromTarget(target: String) {
        val destination = HomeNavigation.parseQuickActionTarget(target) ?: return
        setEffect { HomeContract.Effect.Navigate(destination) }
    }

    private fun loadHomeFeed(refreshing: Boolean) {
        viewModelScope.launch {
            setState {
                if (refreshing) {
                    copy(isRefreshing = true, error = null)
                } else {
                    copy(isLoading = true, error = null)
                }
            }
            when (val result = homeRepository.getHomeFeed()) {
                is Result.Success -> setState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        feed = result.data,
                        error = null,
                    )
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.error,
                        )
                    }
                    setEffect { HomeContract.Effect.ShowError(result.error) }
                }
            }
        }
    }
}
