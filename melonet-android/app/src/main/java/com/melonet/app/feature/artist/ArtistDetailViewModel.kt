package com.melonet.app.feature.artist

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.Song
import com.melonet.app.data.model.SortOption
import com.melonet.app.data.repository.ArtistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModel(
    private val artistRepository: ArtistRepository,
) : BaseViewModel<ArtistDetailContract.State, ArtistDetailContract.Event, ArtistDetailContract.Effect>() {

    private data class SongArgs(val artistId: Int, val sort: SortOption)

    private val argsFlow = MutableStateFlow<SongArgs?>(null)
    private val cachedSongs = mutableListOf<Song>()
    private var artistId: Int = 0

    val songs: Flow<PagingData<Song>> = argsFlow
        .filterNotNull()
        .flatMapLatest { args ->
            artistRepository.artistSongs(args.artistId, args.sort.apiValue)
        }
        .cachedIn(viewModelScope)

    override fun createInitialState() = ArtistDetailContract.State()

    override fun handleEvent(event: ArtistDetailContract.Event) {
        when (event) {
            is ArtistDetailContract.Event.Load -> load(event.artistId)
            is ArtistDetailContract.Event.ToggleFollow -> toggleFollow()
            is ArtistDetailContract.Event.SortSelected -> {
                if (event.sort == uiState.value.sort) return
                setState { copy(sort = event.sort) }
                argsFlow.value = SongArgs(artistId, event.sort)
            }
            is ArtistDetailContract.Event.SongClicked -> {
                setEffect { ArtistDetailContract.Effect.PlayQueue(event.songId) }
            }
        }
    }

    private fun load(id: Int) {
        artistId = id
        argsFlow.value = SongArgs(id, uiState.value.sort)
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = artistRepository.getArtist(id)) {
                is Result.Success -> setState { copy(isLoading = false, artist = result.data) }
                is Result.Error -> setState { copy(isLoading = false, error = result.error) }
            }
        }
    }

    private fun toggleFollow() {
        val artist = uiState.value.artist ?: return
        viewModelScope.launch {
            setState { copy(isFollowLoading = true) }
            val result = if (artist.isFollowing) {
                artistRepository.unfollow(artist.id)
            } else {
                artistRepository.follow(artist.id)
            }
            when (result) {
                is Result.Success -> setState { copy(isFollowLoading = false, artist = result.data) }
                is Result.Error -> {
                    setState { copy(isFollowLoading = false) }
                    setEffect { ArtistDetailContract.Effect.ShowError(result.error) }
                }
            }
        }
    }

    fun updateCachedSongs(songs: List<Song>) {
        cachedSongs.clear()
        cachedSongs.addAll(songs)
    }

    fun getCachedSongs(): List<Song> = cachedSongs.toList()
}
