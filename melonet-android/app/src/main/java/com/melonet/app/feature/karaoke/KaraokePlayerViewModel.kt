package com.melonet.app.feature.karaoke

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.LyricsRepository
import com.melonet.app.data.repository.PlayerRepository
import com.melonet.app.feature.player.PlaybackManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class KaraokePlayerViewModel(
    private val playbackManager: PlaybackManager,
    private val playerRepository: PlayerRepository,
    private val lyricsRepository: LyricsRepository,
) : BaseViewModel<KaraokePlayerContract.State, KaraokePlayerContract.Event, KaraokePlayerContract.Effect>() {

    private var startedSongId: String? = null

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
                        currentLineIndex = computeLineIndex(lyrics.lines, lyrics.synced, position, duration),
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
            }
            if (song != null) {
                setState { copy(song = song) }
                if (current?.id != songId) {
                    playbackManager.play(song, listOf(song))
                }
                playbackManager.setKaraoke(true)
                loadLyrics(song)
            }
        }
    }

    private fun loadLyrics(song: Song) {
        viewModelScope.launch {
            setState { copy(isLoadingLyrics = true) }
            val lyrics = lyricsRepository.getLyrics(
                title = song.title,
                artist = song.artistName,
                durationSec = song.durationSec,
            )
            setState {
                copy(
                    lyrics = lyrics,
                    isLoadingLyrics = false,
                    currentLineIndex = computeLineIndex(
                        lyrics.lines,
                        lyrics.synced,
                        positionMs,
                        durationMs,
                    ),
                )
            }
        }
    }

    override fun handleEvent(event: KaraokePlayerContract.Event) {
        when (event) {
            KaraokePlayerContract.Event.TogglePlayPause -> playbackManager.togglePlayPause()
            is KaraokePlayerContract.Event.SeekTo -> playbackManager.seekTo(event.positionMs)
            KaraokePlayerContract.Event.ToggleVocals -> {
                playbackManager.setKaraoke(!uiState.value.karaokeEnabled)
            }
            is KaraokePlayerContract.Event.LineClicked -> {
                val line = uiState.value.lyrics.lines.getOrNull(event.index) ?: return
                if (line.timeMs >= 0) playbackManager.seekTo(line.timeMs)
            }
        }
    }

    private fun computeLineIndex(
        lines: List<com.melonet.app.data.model.LyricLine>,
        synced: Boolean,
        positionMs: Long,
        durationMs: Long,
    ): Int {
        if (lines.isEmpty()) return -1
        return if (synced) {
            var index = -1
            for (i in lines.indices) {
                if (lines[i].timeMs <= positionMs) index = i else break
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
        // Restore normal (vocal) playback for the global player when leaving.
        playbackManager.setKaraoke(false)
    }
}
