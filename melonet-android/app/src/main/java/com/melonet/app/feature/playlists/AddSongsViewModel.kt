package com.melonet.app.feature.playlists

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.LocalMusicRepository
import com.melonet.app.data.repository.PlaylistRepository
import com.melonet.app.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddSongsViewModel(
    private val playlistRepository: PlaylistRepository,
    private val localMusicRepository: LocalMusicRepository,
    private val searchRepository: SearchRepository,
) : BaseViewModel<AddSongsContract.State, AddSongsContract.Event, AddSongsContract.Effect>() {

    private var searchJob: Job? = null

    override fun createInitialState() = AddSongsContract.State()

    override fun handleEvent(event: AddSongsContract.Event) {
        when (event) {
            is AddSongsContract.Event.Init -> {
                setState { copy(playlistId = event.playlistId) }
                loadExistingSongs(event.playlistId)
            }
            is AddSongsContract.Event.TabSelected -> setState { copy(selectedTab = event.tab) }
            is AddSongsContract.Event.SearchQueryChanged -> {
                setState { copy(appSearchQuery = event.query) }
                if (uiState.value.selectedTab == AddSongsContract.Tab.APP) {
                    searchAppSongs(event.query)
                }
            }
            is AddSongsContract.Event.AddSong -> addSong(event.song)
            AddSongsContract.Event.LocalPermissionGranted -> {
                setState { copy(hasLocalPermission = true) }
                loadLocalSongs()
            }
            AddSongsContract.Event.LocalPermissionDenied -> {
                setState { copy(hasLocalPermission = false) }
            }
            AddSongsContract.Event.Dismiss -> setEffect { AddSongsContract.Effect.NavigateBack }
        }
    }

    private fun loadExistingSongs(playlistId: Int) {
        viewModelScope.launch {
            val localSongs = playlistRepository.getLocalPlaylistSongs(playlistId)
            setState { copy(addedSongIds = localSongs.map { it.id }.toSet()) }
        }
    }

    private fun loadLocalSongs() {
        viewModelScope.launch {
            setState { copy(isLoadingLocal = true) }
            val songs = localMusicRepository.getLocalSongs()
            setState { copy(isLoadingLocal = false, localSongs = songs) }
        }
    }

    private fun searchAppSongs(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            setState { copy(appSearchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            setState { copy(isSearching = true) }
            val songs = searchRepository.searchSongs(trimmed)
            setState { copy(appSearchResults = songs, isSearching = false) }
        }
    }

    private fun addSong(song: Song) {
        val playlistId = uiState.value.playlistId
        if (playlistId == 0) return
        viewModelScope.launch {
            when (playlistRepository.addLocalSongToPlaylist(playlistId, song)) {
                is Result.Success -> {
                    setState { copy(addedSongIds = addedSongIds + song.id) }
                    setEffect { AddSongsContract.Effect.SongAdded(song.title) }
                }
                is Result.Error -> Unit
            }
        }
    }
}
