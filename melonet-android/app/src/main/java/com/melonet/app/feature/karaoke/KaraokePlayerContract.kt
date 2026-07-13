package com.melonet.app.feature.karaoke

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Lyrics
import com.melonet.app.data.model.Song

object KaraokePlayerContract {

    data class State(
        val song: Song? = null,
        val isPlaying: Boolean = false,
        val isLoading: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val lyrics: Lyrics = Lyrics.EMPTY,
        val isLoadingLyrics: Boolean = true,
        val currentLineIndex: Int = -1,
        val karaokeEnabled: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data object TogglePlayPause : Event
        data class SeekTo(val positionMs: Long) : Event
        data object ToggleVocals : Event
        data class LineClicked(val index: Int) : Event
    }

    sealed interface Effect : UiEffect
}
