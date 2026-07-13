package com.melonet.app.feature.localmusic

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Song

object LocalMusicContract {

    data class State(
        val songs: List<Song> = emptyList(),
        val isLoading: Boolean = false,
        val hasPermission: Boolean = false,
        val permissionRequested: Boolean = false,
        val searchQuery: String = "",
    ) : UiState {
        val filteredSongs: List<Song>
            get() = if (searchQuery.isBlank()) {
                songs
            } else {
                val query = searchQuery.trim().lowercase()
                songs.filter {
                    it.title.lowercase().contains(query) ||
                        it.artistName.lowercase().contains(query) ||
                        it.albumTitle.orEmpty().lowercase().contains(query)
                }
            }
    }

    sealed interface Event : UiEvent {
        data object Load : Event
        data object PermissionGranted : Event
        data object PermissionDenied : Event
        data class SearchQueryChanged(val query: String) : Event
        data class SongClicked(val song: Song) : Event
        data object PlayAll : Event
        data object ShuffleAll : Event
    }

    sealed interface Effect : UiEffect {
        data class PlaySong(val song: Song, val queue: List<Song>) : Effect
    }
}
