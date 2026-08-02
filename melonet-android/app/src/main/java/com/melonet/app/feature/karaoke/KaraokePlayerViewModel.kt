package com.melonet.app.feature.karaoke

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.KaraokeRecordingRepository
import com.melonet.app.data.repository.LyricsRepository
import com.melonet.app.data.repository.PlayerRepository
import com.melonet.app.feature.player.PlaybackManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KaraokePlayerViewModel(
    private val playbackManager: PlaybackManager,
    private val playerRepository: PlayerRepository,
    private val lyricsRepository: LyricsRepository,
    private val karaokeRecordingRepository: KaraokeRecordingRepository,
) : BaseViewModel<KaraokePlayerContract.State, KaraokePlayerContract.Event, KaraokePlayerContract.Effect>() {

    private var startedSongId: String? = null
    private var recordingTicker: Job? = null
    private var countdownJob: Job? = null
    private var recordingStartedAtMs: Long = 0L

    override fun createInitialState() = KaraokePlayerContract.State()

    init {
        playbackManager.connect()
        playbackManager.state
            .onEach { playback ->
                setState {
                    val duration = playback.durationMs
                    val position = playback.positionMs
                    copy(
                        song = playback.currentSong ?: song,
                        isPlaying = playback.isPlaying,
                        isLoading = playback.isLoading,
                        positionMs = position,
                        durationMs = duration,
                        karaokeEnabled = playback.karaokeEnabled,
                        currentLineIndex = computeLineIndex(
                            lyrics.lines,
                            lyrics.synced,
                            position,
                            duration,
                            lyricsOffsetMs,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun start(songId: String) {
        if (startedSongId == songId) return
        startedSongId = songId
        viewModelScope.launch {
            val current = playbackManager.state.value.currentSong
            val song = if (current?.id == songId) {
                current
            } else {
                when (val result = playerRepository.getSong(songId)) {
                    is Result.Success -> result.data
                    is Result.Error -> null
                }
            } ?: return@launch

            setState {
                copy(
                    song = song,
                    isLoadingLyrics = true,
                    lyricsReady = false,
                    lyricsOffsetMs = 0L,
                )
            }
            // Prepare audio paused — only start once lyrics are ready (or failed).
            playbackManager.preparePaused(song, listOf(song))
            playbackManager.setKaraoke(true)

            val lyrics = lyricsRepository.getLyrics(
                title = song.title,
                artist = song.artistName,
                durationSec = song.durationSec,
                album = song.albumTitle,
                embeddedLyrics = song.lyrics.takeIf { it.isNotBlank() },
                syncedOnly = true,
            )
            setState {
                copy(
                    lyrics = lyrics,
                    isLoadingLyrics = false,
                    lyricsReady = true,
                    currentLineIndex = computeLineIndex(
                        lyrics.lines,
                        lyrics.synced,
                        0L,
                        durationMs,
                        lyricsOffsetMs,
                    ),
                )
            }
            // Start from the beginning together with lyrics highlighting.
            playbackManager.seekTo(0)
            playbackManager.resume()
        }
    }

    override fun handleEvent(event: KaraokePlayerContract.Event) {
        when (event) {
            KaraokePlayerContract.Event.TogglePlayPause -> {
                if (!uiState.value.lyricsReady || uiState.value.countdownSeconds != null) return
                playbackManager.togglePlayPause()
            }
            is KaraokePlayerContract.Event.SeekTo -> {
                if (!uiState.value.lyricsReady) return
                playbackManager.seekTo(event.positionMs)
            }
            KaraokePlayerContract.Event.ToggleVocals -> {
                playbackManager.setKaraoke(!uiState.value.karaokeEnabled)
            }
            is KaraokePlayerContract.Event.LineClicked -> {
                val line = uiState.value.lyrics.lines.getOrNull(event.index) ?: return
                if (line.timeMs >= 0) {
                    playbackManager.seekTo((line.timeMs - uiState.value.lyricsOffsetMs).coerceAtLeast(0L))
                }
            }
            KaraokePlayerContract.Event.StartRecording -> {
                setEffect { KaraokePlayerContract.Effect.RequestMicPermission }
            }
            KaraokePlayerContract.Event.PermissionGranted -> beginCountdownAndRecord()
            KaraokePlayerContract.Event.PermissionDenied -> {
                setState { copy(permissionNeeded = true) }
                setEffect { KaraokePlayerContract.Effect.ShowMessage("mic_permission_denied") }
            }
            KaraokePlayerContract.Event.StopRecording -> finishRecording()
            KaraokePlayerContract.Event.NudgeOffsetEarlier -> adjustOffset(-250L)
            KaraokePlayerContract.Event.NudgeOffsetLater -> adjustOffset(250L)
            KaraokePlayerContract.Event.ResetOffset -> adjustOffset(-uiState.value.lyricsOffsetMs)
        }
    }

    private fun adjustOffset(deltaMs: Long) {
        setState {
            val next = (lyricsOffsetMs + deltaMs).coerceIn(-10_000L, 10_000L)
            copy(
                lyricsOffsetMs = next,
                currentLineIndex = computeLineIndex(
                    lyrics.lines,
                    lyrics.synced,
                    positionMs,
                    durationMs,
                    next,
                ),
            )
        }
    }

    private fun beginCountdownAndRecord() {
        val song = uiState.value.song ?: return
        if (uiState.value.isRecording || uiState.value.countdownSeconds != null) return
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            // Pause briefly so singer can prepare; resume when mic starts.
            playbackManager.pause()
            for (sec in 3 downTo 1) {
                setState { copy(countdownSeconds = sec) }
                delay(1_000)
            }
            setState { copy(countdownSeconds = null) }
            beginRecording(song)
        }
    }

    private fun beginRecording(song: Song) {
        if (uiState.value.isRecording) return
        runCatching {
            playbackManager.setHandleAudioFocus(false)
            karaokeRecordingRepository.startRecording()
            recordingStartedAtMs = System.currentTimeMillis()
            setState { copy(isRecording = true, recordingSeconds = 0, permissionNeeded = false) }
            if (uiState.value.lyricsReady) {
                playbackManager.resume()
            }
            recordingTicker?.cancel()
            recordingTicker = viewModelScope.launch {
                while (isActive) {
                    delay(1000)
                    val secs = ((System.currentTimeMillis() - recordingStartedAtMs) / 1000).toInt()
                    setState { copy(recordingSeconds = secs) }
                }
            }
        }.onFailure {
            playbackManager.setHandleAudioFocus(true)
            setEffect { KaraokePlayerContract.Effect.ShowMessage("record_failed") }
        }
    }

    private fun finishRecording() {
        if (!uiState.value.isRecording) return
        recordingTicker?.cancel()
        countdownJob?.cancel()
        val song = uiState.value.song ?: return
        val file = karaokeRecordingRepository.stopRecording()
        playbackManager.setHandleAudioFocus(true)
        setState { copy(isRecording = false, countdownSeconds = null) }
        if (file == null || !file.exists()) {
            setEffect { KaraokePlayerContract.Effect.ShowMessage("record_failed") }
            return
        }
        viewModelScope.launch {
            val durationSec = uiState.value.recordingSeconds.coerceAtLeast(
                (uiState.value.positionMs / 1000).toInt().coerceAtLeast(1),
            )
            val saved = karaokeRecordingRepository.saveRecording(song, file, durationSec)
            setEffect { KaraokePlayerContract.Effect.RecordingSaved(saved.id) }
            setEffect { KaraokePlayerContract.Effect.ShowMessage("record_saved") }
        }
    }

    private fun computeLineIndex(
        lines: List<com.melonet.app.data.model.LyricLine>,
        synced: Boolean,
        positionMs: Long,
        durationMs: Long,
        offsetMs: Long,
    ): Int {
        if (lines.isEmpty()) return -1
        val effective = positionMs + offsetMs
        return if (synced) {
            var index = -1
            for (i in lines.indices) {
                if (lines[i].timeMs <= effective) index = i else break
            }
            index
        } else {
            if (durationMs <= 0) 0
            else ((positionMs.toFloat() / durationMs) * lines.size)
                .toInt()
                .coerceIn(0, lines.size - 1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        if (uiState.value.isRecording) {
            karaokeRecordingRepository.cancelRecording()
        }
        recordingTicker?.cancel()
        playbackManager.setHandleAudioFocus(true)
        playbackManager.setKaraoke(false)
    }
}
