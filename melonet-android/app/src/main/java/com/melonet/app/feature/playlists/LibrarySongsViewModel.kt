package com.melonet.app.feature.playlists

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySongsViewModel(
    private val libraryRepository: LibraryRepository,
) : BaseViewModel<LibrarySongsContract.State, LibrarySongsContract.Event, LibrarySongsContract.Effect>() {

    private val listTypeFlow = MutableStateFlow(LibraryListType.LIKED)
    private val cachedSongs = mutableListOf<Song>()

    val songs: Flow<PagingData<Song>> = listTypeFlow
        .flatMapLatest { type ->
            when (type) {
                LibraryListType.LIKED -> libraryRepository.likedSongs()
                LibraryListType.RECENT -> libraryRepository.recentSongs()
            }
        }
        .cachedIn(viewModelScope)

    override fun createInitialState() = LibrarySongsContract.State()

    fun setListType(type: LibraryListType) {
        listTypeFlow.value = type
        setState { copy(listType = type) }
    }

    override fun handleEvent(event: LibrarySongsContract.Event) {
        when (event) {
            is LibrarySongsContract.Event.SongClicked -> {
                setEffect { LibrarySongsContract.Effect.NavigateToPlayer(event.songId) }
            }
            is LibrarySongsContract.Event.DismissSong -> dismissSong(event.songId)
            LibrarySongsContract.Event.PlayAll -> {
                val first = cachedSongs.firstOrNull()?.id ?: return
                setEffect { LibrarySongsContract.Effect.PlayQueue(first, shuffle = false) }
            }
            LibrarySongsContract.Event.ShuffleAll -> {
                val first = cachedSongs.shuffled().firstOrNull()?.id ?: return
                setEffect { LibrarySongsContract.Effect.PlayQueue(first, shuffle = true) }
            }
        }
    }

    fun updateCachedSongs(songs: List<Song>) {
        cachedSongs.clear()
        cachedSongs.addAll(songs)
    }

    fun getCachedSongs(): List<Song> = cachedSongs.toList()

    private fun dismissSong(songId: Int) {
        viewModelScope.launch {
            when (uiState.value.listType) {
                LibraryListType.LIKED -> libraryRepository.unlikeSong(songId)
                LibraryListType.RECENT -> libraryRepository.dismissRecentSong(songId)
            }
        }
    }
}
