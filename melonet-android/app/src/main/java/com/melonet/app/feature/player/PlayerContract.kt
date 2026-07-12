package com.melonet.app.feature.player

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Song

object PlayerContract {

    data class State(
        val currentSong: Song? = null,
        val queue: List<Song> = emptyList(),
        val isPlaying: Boolean = false,
        val isLoading: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val playbackSpeed: Float = 1f,
        val sleepTimerMinutesLeft: Int? = null,
        val showSpeedDialog: Boolean = false,
        val showSleepTimerDialog: Boolean = false,
        val gradientColors: List<Long> = emptyList(),
    ) : UiState

    sealed interface Event : UiEvent {
        data class PlaySong(val song: Song, val queue: List<Song> = listOf()) : Event
        data class PlaySongId(val songId: Int, val queue: List<Song> = emptyList()) : Event
        data object TogglePlayPause : Event
        data object SkipNext : Event
        data object SkipPrevious : Event
        data class SeekTo(val positionMs: Long) : Event
        data class SetSpeed(val speed: Float) : Event
        data class SetSleepTimer(val minutes: Int?) : Event
        data object ShowSpeedDialog : Event
        data object HideSpeedDialog : Event
        data object ShowSleepTimerDialog : Event
        data object HideSleepTimerDialog : Event
        data class UpdateGradient(val colors: List<Long>) : Event
    }

    sealed interface Effect : UiEffect {
        data object NavigateBack : Effect
    }
}
