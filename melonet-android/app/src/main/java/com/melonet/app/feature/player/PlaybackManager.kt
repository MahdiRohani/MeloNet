package com.melonet.app.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.PlayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PlaybackState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val sleepTimerMinutesLeft: Int? = null,
    val isConnected: Boolean = false,
)

class PlaybackManager(
    private val context: Context,
    private val playerRepository: PlayerRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var playRecordedForSongId: Int? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isLoading = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_READY) {
                controller?.let { c ->
                    _state.update { it.copy(durationMs = c.duration.coerceAtLeast(0L)) }
                    maybeRecordPlay()
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSongFromPlayer()
        }

        override fun onPlaybackParametersChanged(
            playbackParameters: androidx.media3.common.PlaybackParameters,
        ) {
            _state.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, MelonetPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener({
            try {
                val c = controllerFuture?.get() ?: return@addListener
                controller = c
                c.addListener(playerListener)
                _state.update {
                    it.copy(isConnected = true, playbackSpeed = c.playbackParameters.speed)
                }
                syncFromPlayer(c)
                if (c.isPlaying) startProgressUpdates()
            } catch (_: Exception) {
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        stopProgressUpdates()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        _state.update { it.copy(isConnected = false) }
    }

    fun play(song: Song, queue: List<Song> = listOf(song)) {
        scope.launch {
            connectAndAwait()
            val c = controller ?: return@launch
            val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            playRecordedForSongId = null
            c.setMediaItems(queue.map(::buildMediaItem), startIndex, 0L)
            c.prepare()
            c.play()
            _state.update { it.copy(currentSong = song, queue = queue) }
        }
    }

    fun playSongId(songId: Int, queue: List<Song> = emptyList()) {
        scope.launch {
            val existing = _state.value.queue.find { it.id == songId }
                ?: _state.value.currentSong?.takeIf { it.id == songId }
            val song = existing ?: when (val result = playerRepository.getSong(songId)) {
                is Result.Success -> result.data
                is Result.Error -> return@launch
            }
            val songs = queue.ifEmpty { listOf(song) }
            play(song, songs)
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null) {
            _state.update { it.copy(sleepTimerMinutesLeft = null) }
            return
        }
        _state.update { it.copy(sleepTimerMinutesLeft = minutes) }
        sleepTimerJob = scope.launch {
            var left = minutes
            while (left > 0) {
                delay(60_000L)
                left--
                _state.update { state ->
                    state.copy(sleepTimerMinutesLeft = left.takeIf { remaining -> remaining > 0 })
                }
            }
            controller?.pause()
            _state.update { it.copy(sleepTimerMinutesLeft = null) }
        }
    }

    private fun buildMediaItem(song: Song): MediaItem {
        val uri = playerRepository.resolveAudioUri(song)
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artistName)
                    .apply {
                        if (song.coverUrl.isNotBlank()) {
                            setArtworkUri(song.coverUrl.toUri())
                        }
                    }
                    .build(),
            )
            .build()
    }

    private suspend fun connectAndAwait() {
        if (controller != null) return
        connect()
        val future = controllerFuture ?: return
        suspendCancellableCoroutine { cont ->
            future.addListener({
                try {
                    future.get()
                    cont.resume(Unit)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }, MoreExecutors.directExecutor())
        }
    }

    private fun syncFromPlayer(c: MediaController) {
        updateCurrentSongFromPlayer()
        _state.update {
            it.copy(
                isPlaying = c.isPlaying,
                isLoading = c.playbackState == Player.STATE_BUFFERING,
                positionMs = c.currentPosition.coerceAtLeast(0L),
                durationMs = c.duration.coerceAtLeast(0L),
            )
        }
    }

    private fun updateCurrentSongFromPlayer() {
        val songId = controller?.currentMediaItem?.mediaId?.toIntOrNull() ?: return
        val song = _state.value.queue.find { it.id == songId }
            ?: _state.value.currentSong?.takeIf { it.id == songId }
        if (song != null) {
            _state.update { it.copy(currentSong = song) }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                controller?.let { c ->
                    _state.update {
                        it.copy(
                            positionMs = c.currentPosition.coerceAtLeast(0L),
                            durationMs = c.duration.coerceAtLeast(0L),
                        )
                    }
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
    }

    private fun maybeRecordPlay() {
        val song = _state.value.currentSong ?: return
        if (playRecordedForSongId == song.id) return
        playRecordedForSongId = song.id
        scope.launch {
            playerRepository.recordPlay(
                songId = song.id,
                durationSec = 0,
                source = "player",
            )
        }
    }
}
