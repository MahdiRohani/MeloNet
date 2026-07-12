package com.melonet.app.feature.playlists

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.PlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailViewModel(
    private val playlistRepository: PlaylistRepository,
) : BaseViewModel<PlaylistDetailContract.State, PlaylistDetailContract.Event, PlaylistDetailContract.Effect>() {

    private val playlistIdFlow = MutableStateFlow<Int?>(null)
    private val cachedSongs = mutableListOf<Song>()

    val songs: Flow<PagingData<Song>> = playlistIdFlow
        .flatMapLatest { id ->
            if (id == null) {
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                playlistRepository.playlistSongs(id)
            }
        }
        .cachedIn(viewModelScope)

    override fun createInitialState() = PlaylistDetailContract.State()

    override fun handleEvent(event: PlaylistDetailContract.Event) {
        when (event) {
            is PlaylistDetailContract.Event.Load -> loadPlaylist(event.playlistId)
            is PlaylistDetailContract.Event.SongClicked -> {
                setEffect { PlaylistDetailContract.Effect.NavigateToPlayer(event.songId) }
            }
            PlaylistDetailContract.Event.PlayAll -> {
                val first = cachedSongs.firstOrNull()?.id ?: return
                setEffect {
                    PlaylistDetailContract.Effect.PlayQueue(startSongId = first, shuffle = false)
                }
            }
            PlaylistDetailContract.Event.ShuffleAll -> {
                val first = cachedSongs.shuffled().firstOrNull()?.id ?: return
                setEffect {
                    PlaylistDetailContract.Effect.PlayQueue(startSongId = first, shuffle = true)
                }
            }
        }
    }

    fun updateCachedSongs(songs: List<Song>) {
        cachedSongs.clear()
        cachedSongs.addAll(songs)
    }

    fun getCachedSongs(): List<Song> = cachedSongs.toList()

    private fun loadPlaylist(playlistId: Int) {
        playlistIdFlow.value = playlistId
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = playlistRepository.getPlaylist(playlistId)) {
                is Result.Success -> setState {
                    copy(isLoading = false, playlist = result.data, error = null)
                }
                is Result.Error -> {
                    setState { copy(isLoading = false, error = result.error) }
                    setEffect { PlaylistDetailContract.Effect.ShowError(result.error) }
                }
            }
        }
    }
}
