package com.melonet.app.feature.localmusic

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.repository.LocalMusicRepository
import kotlinx.coroutines.launch

class LocalMusicViewModel(
    private val localMusicRepository: LocalMusicRepository,
) : BaseViewModel<LocalMusicContract.State, LocalMusicContract.Event, LocalMusicContract.Effect>() {

    override fun createInitialState() = LocalMusicContract.State()

    override fun handleEvent(event: LocalMusicContract.Event) {
        when (event) {
            LocalMusicContract.Event.Load -> loadSongs()
            LocalMusicContract.Event.PermissionGranted -> {
                setState { copy(hasPermission = true, permissionRequested = true) }
                loadSongs()
            }
            LocalMusicContract.Event.PermissionDenied -> {
                setState { copy(hasPermission = false, permissionRequested = true) }
            }
            is LocalMusicContract.Event.SearchQueryChanged -> {
                setState { copy(searchQuery = event.query) }
            }
            is LocalMusicContract.Event.SongClicked -> {
                val queue = uiState.value.filteredSongs
                setEffect { LocalMusicContract.Effect.PlaySong(event.song, queue) }
            }
            LocalMusicContract.Event.PlayAll -> {
                val songs = uiState.value.filteredSongs
                val first = songs.firstOrNull() ?: return
                setEffect { LocalMusicContract.Effect.PlaySong(first, songs) }
            }
            LocalMusicContract.Event.ShuffleAll -> {
                val songs = uiState.value.filteredSongs.shuffled()
                val first = songs.firstOrNull() ?: return
                setEffect { LocalMusicContract.Effect.PlaySong(first, songs) }
            }
        }
    }

    private fun loadSongs() {
        if (!uiState.value.hasPermission) return
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            val songs = localMusicRepository.getLocalSongs()
            setState { copy(isLoading = false, songs = songs) }
        }
    }
}
