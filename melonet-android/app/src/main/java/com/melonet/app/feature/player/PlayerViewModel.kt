package com.melonet.app.feature.player

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.Song
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playbackManager: PlaybackManager,
) : BaseViewModel<PlayerContract.State, PlayerContract.Event, PlayerContract.Effect>() {

    override fun createInitialState() = PlayerContract.State()

    init {
        playbackManager.connect()
        playbackManager.state
            .onEach { playback ->
                setState {
                    copy(
                        currentSong = playback.currentSong,
                        queue = playback.queue,
                        isPlaying = playback.isPlaying,
                        isLoading = playback.isLoading,
                        positionMs = playback.positionMs,
                        durationMs = playback.durationMs,
                        playbackSpeed = playback.playbackSpeed,
                        sleepTimerMinutesLeft = playback.sleepTimerMinutesLeft,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: PlayerContract.Event) {
        when (event) {
            is PlayerContract.Event.PlaySong -> {
                val queue = event.queue.ifEmpty { listOf(event.song) }
                playbackManager.play(event.song, queue)
            }
            is PlayerContract.Event.PlaySongId -> {
                playbackManager.playSongId(event.songId, event.queue)
            }
            PlayerContract.Event.TogglePlayPause -> playbackManager.togglePlayPause()
            PlayerContract.Event.SkipNext -> playbackManager.skipNext()
            PlayerContract.Event.SkipPrevious -> playbackManager.skipPrevious()
            is PlayerContract.Event.SeekTo -> playbackManager.seekTo(event.positionMs)
            is PlayerContract.Event.SetSpeed -> {
                playbackManager.setSpeed(event.speed)
                setState { copy(showSpeedDialog = false) }
            }
            is PlayerContract.Event.SetSleepTimer -> {
                playbackManager.setSleepTimer(event.minutes)
                setState { copy(showSleepTimerDialog = false) }
            }
            PlayerContract.Event.ShowSpeedDialog -> setState { copy(showSpeedDialog = true) }
            PlayerContract.Event.HideSpeedDialog -> setState { copy(showSpeedDialog = false) }
            PlayerContract.Event.ShowSleepTimerDialog -> setState { copy(showSleepTimerDialog = true) }
            PlayerContract.Event.HideSleepTimerDialog -> setState { copy(showSleepTimerDialog = false) }
            is PlayerContract.Event.UpdateGradient -> setState { copy(gradientColors = event.colors) }
        }
    }

    fun playIfNeeded(songId: Int) {
        viewModelScope.launch {
            val current = uiState.value.currentSong
            if (current?.id != songId) {
                handleEvent(PlayerContract.Event.PlaySongId(songId))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
