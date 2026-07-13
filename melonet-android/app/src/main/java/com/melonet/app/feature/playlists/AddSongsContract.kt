package com.melonet.app.feature.playlists

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Song

object AddSongsContract {

    enum class Tab {
        DEVICE,
        APP,
    }

    data class State(
        val playlistId: Int = 0,
        val selectedTab: Tab = Tab.DEVICE,
        val localSongs: List<Song> = emptyList(),
        val appSearchQuery: String = "",
        val appSearchResults: List<Song> = emptyList(),
        val addedSongIds: Set<String> = emptySet(),
        val isLoadingLocal: Boolean = false,
        val isSearching: Boolean = false,
        val hasLocalPermission: Boolean = false,
    ) : UiState {
        val filteredLocalSongs: List<Song>
            get() = if (appSearchQuery.isBlank() || selectedTab != Tab.DEVICE) {
                localSongs
            } else {
                val query = appSearchQuery.trim().lowercase()
                localSongs.filter {
                    it.title.lowercase().contains(query) ||
                        it.artistName.lowercase().contains(query)
                }
            }
    }

    sealed interface Event : UiEvent {
        data class Init(val playlistId: Int) : Event
        data class TabSelected(val tab: Tab) : Event
        data class SearchQueryChanged(val query: String) : Event
        data class AddSong(val song: Song) : Event
        data object LocalPermissionGranted : Event
        data object LocalPermissionDenied : Event
        data object Dismiss : Event
    }

    sealed interface Effect : UiEffect {
        data object NavigateBack : Effect
        data class SongAdded(val songTitle: String) : Effect
    }
}
