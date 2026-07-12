package com.melonet.app.feature.playlists

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState

enum class LibraryListType {
    LIKED,
    RECENT,
}

object LibrarySongsContract {

    data class State(
        val listType: LibraryListType = LibraryListType.LIKED,
    ) : UiState

    sealed interface Event : UiEvent {
        data class SongClicked(val songId: String) : Event
        data class DismissSong(val songId: String) : Event
        data object PlayAll : Event
        data object ShuffleAll : Event
    }

    sealed interface Effect : UiEffect {
        data class NavigateToPlayer(val songId: String) : Effect
        data class PlayQueue(val startSongId: String, val shuffle: Boolean) : Effect
    }
}
