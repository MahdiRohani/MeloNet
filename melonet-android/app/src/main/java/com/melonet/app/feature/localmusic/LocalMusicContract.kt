package com.melonet.app.feature.localmusic

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Song

enum class LocalMusicTab { Songs, Albums, Artists }

data class LocalMusicGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String?,
    val songs: List<Song>,
)

object LocalMusicContract {

    data class State(
        val songs: List<Song> = emptyList(),
        val isLoading: Boolean = false,
        val hasPermission: Boolean = false,
        val permissionRequested: Boolean = false,
        val searchQuery: String = "",
        val selectedTab: LocalMusicTab = LocalMusicTab.Songs,
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

        val albumGroups: List<LocalMusicGroup>
            get() = filteredSongs
                .groupBy { it.albumTitle?.takeIf { title -> title.isNotBlank() } ?: "Unknown Album" }
                .map { (album, tracks) ->
                    LocalMusicGroup(
                        key = "album_$album",
                        title = album,
                        subtitle = tracks.firstOrNull()?.artistName.orEmpty(),
                        coverUrl = tracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
                            ?: tracks.firstOrNull()?.coverUrl,
                        songs = tracks.sortedBy { it.title.lowercase() },
                    )
                }
                .sortedBy { it.title.lowercase() }

        val artistGroups: List<LocalMusicGroup>
            get() = filteredSongs
                .groupBy { it.artistName.ifBlank { "Unknown Artist" } }
                .map { (artist, tracks) ->
                    LocalMusicGroup(
                        key = "artist_$artist",
                        title = artist,
                        subtitle = "",
                        coverUrl = tracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl
                            ?: tracks.firstOrNull()?.coverUrl,
                        songs = tracks.sortedBy { it.title.lowercase() },
                    )
                }
                .sortedBy { it.title.lowercase() }
    }

    sealed interface Event : UiEvent {
        data object Load : Event
        data object PermissionGranted : Event
        data object PermissionDenied : Event
        data class SearchQueryChanged(val query: String) : Event
        data class TabSelected(val tab: LocalMusicTab) : Event
        data class SongClicked(val song: Song) : Event
        data class GroupClicked(val group: LocalMusicGroup) : Event
        data object PlayAll : Event
        data object ShuffleAll : Event
    }

    sealed interface Effect : UiEffect {
        data class PlaySong(val song: Song, val queue: List<Song>) : Effect
    }
}
