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
        val lyricsReady: Boolean = false,
        val currentLineIndex: Int = -1,
        /** Manual LRC calibration in milliseconds (positive = lyrics later). */
        val lyricsOffsetMs: Long = 0L,
        val karaokeEnabled: Boolean = true,
        val isRecording: Boolean = false,
        /** 3..1 during pre-roll; null when idle. */
        val countdownSeconds: Int? = null,
        val recordingSeconds: Int = 0,
        val permissionNeeded: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object TogglePlayPause : Event
        data class SeekTo(val positionMs: Long) : Event
        data object ToggleVocals : Event
        data class LineClicked(val index: Int) : Event
        data object StartRecording : Event
        data object StopRecording : Event
        data object PermissionGranted : Event
        data object PermissionDenied : Event
        data object NudgeOffsetEarlier : Event
        data object NudgeOffsetLater : Event
        data object ResetOffset : Event
    }

    sealed interface Effect : UiEffect {
        data object RequestMicPermission : Effect
        data class RecordingSaved(val recordingId: Long) : Effect
        data class ShowMessage(val message: String) : Effect
    }
}
