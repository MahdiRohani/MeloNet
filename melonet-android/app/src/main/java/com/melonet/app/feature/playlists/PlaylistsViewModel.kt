package com.melonet.app.feature.playlists

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.Playlist
import com.melonet.app.data.model.PlaylistScope
import com.melonet.app.data.repository.PlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModel(
    private val playlistRepository: PlaylistRepository,
) : BaseViewModel<PlaylistsContract.State, PlaylistsContract.Event, PlaylistsContract.Effect>() {

    private val refreshTrigger = MutableStateFlow(0)

    val systemPlaylistsFlow: Flow<PagingData<Playlist>> = refreshTrigger
        .flatMapLatest { playlistRepository.playlists(PlaylistScope.SYSTEM) }
        .cachedIn(viewModelScope)

    val userPlaylistsFlow: Flow<PagingData<Playlist>> = refreshTrigger
        .flatMapLatest { playlistRepository.playlists(PlaylistScope.MINE) }
        .cachedIn(viewModelScope)

    override fun createInitialState() = PlaylistsContract.State(isLoading = false)

    override fun handleEvent(event: PlaylistsContract.Event) {
        when (event) {
            PlaylistsContract.Event.Load,
            PlaylistsContract.Event.Refresh -> refreshTrigger.value++
            is PlaylistsContract.Event.PlaylistClicked -> {
                setEffect { PlaylistsContract.Effect.NavigateToDetail(event.playlist.id) }
            }
            PlaylistsContract.Event.ShowCreateDialog -> setState { copy(showCreateDialog = true) }
            PlaylistsContract.Event.HideCreateDialog -> {
                setState { copy(showCreateDialog = false, createTitle = "") }
            }
            is PlaylistsContract.Event.CreateTitleChanged -> {
                setState { copy(createTitle = event.title) }
            }
            PlaylistsContract.Event.CreatePlaylist -> createPlaylist()
            is PlaylistsContract.Event.DeletePlaylist -> deletePlaylist(event.playlist)
            PlaylistsContract.Event.NavigateToLiked -> {
                setEffect { PlaylistsContract.Effect.NavigateToLiked }
            }
            PlaylistsContract.Event.NavigateToRecent -> {
                setEffect { PlaylistsContract.Effect.NavigateToRecent }
            }
        }
    }

    private fun createPlaylist() {
        val title = uiState.value.createTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            setState { copy(isCreating = true) }
            when (val result = playlistRepository.createPlaylist(title)) {
                is Result.Success -> {
                    setState { copy(isCreating = false, showCreateDialog = false, createTitle = "") }
                    refreshTrigger.value++
                }
                is Result.Error -> {
                    setState { copy(isCreating = false) }
                    setEffect { PlaylistsContract.Effect.ShowError(result.error) }
                }
            }
        }
    }

    private fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            when (val result = playlistRepository.deletePlaylist(playlist.id)) {
                is Result.Success -> refreshTrigger.value++
                is Result.Error -> setEffect { PlaylistsContract.Effect.ShowError(result.error) }
            }
        }
    }
}
