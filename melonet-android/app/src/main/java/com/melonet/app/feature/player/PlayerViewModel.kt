package com.melonet.app.feature.player

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.DownloadStatus
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.DownloadRepository
import com.melonet.app.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val playbackManager: PlaybackManager,
    private val downloadRepository: DownloadRepository,
    private val userRepository: UserRepository,
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
                        shuffleEnabled = playback.shuffleEnabled,
                        repeatMode = playback.repeatMode,
                    )
                }
            }
            .launchIn(viewModelScope)

        userRepository.isPremiumFlow
            .onEach { isPremium -> setState { copy(isPremium = isPremium) } }
            .launchIn(viewModelScope)

        uiState
            .map { it.currentSong?.id }
            .distinctUntilChanged()
            .flatMapLatest { songId ->
                if (songId == null) {
                    flowOf(null)
                } else {
                    downloadRepository.observeDownload(songId).map { it?.status }
                }
            }
            .onEach { status -> setState { copy(downloadStatus = status) } }
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
            PlayerContract.Event.DownloadClicked -> handleDownloadClick()
            PlayerContract.Event.DismissUpgradeDialog -> setState { copy(showUpgradeDialog = false) }
            PlayerContract.Event.ToggleShuffle -> playbackManager.toggleShuffle()
            PlayerContract.Event.CycleRepeatMode -> playbackManager.cycleRepeatMode()
        }
    }

    fun playIfNeeded(songId: String) {
        viewModelScope.launch {
            val current = uiState.value.currentSong
            if (current?.id != songId) {
                handleEvent(PlayerContract.Event.PlaySongId(songId))
            }
        }
    }

    private fun handleDownloadClick() {
        val song = uiState.value.currentSong ?: return
        if (!uiState.value.isPremium) {
            setState { copy(showUpgradeDialog = true) }
            return
        }
        when (uiState.value.downloadStatus) {
            DownloadStatus.COMPLETED, DownloadStatus.DOWNLOADING, DownloadStatus.PENDING -> return
            DownloadStatus.FAILED, null -> viewModelScope.launch {
                downloadRepository.enqueueDownload(song)
            }
        }
    }

    fun upgradePremium() {
        viewModelScope.launch {
            userRepository.setPremiumStatus(true)
            setState { copy(isPremium = true, showUpgradeDialog = false) }
            uiState.value.currentSong?.let { song ->
                downloadRepository.enqueueDownload(song)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
