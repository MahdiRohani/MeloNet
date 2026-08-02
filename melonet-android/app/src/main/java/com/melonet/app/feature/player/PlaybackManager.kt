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
import com.melonet.app.data.local.SettingsRepository
import com.melonet.app.data.model.RepeatMode
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
    /** True only while initially buffering a newly started track — not during seek. */
    val isLoading: Boolean = false,
    val isSeeking: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val sleepTimerMinutesLeft: Int? = null,
    val isConnected: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val karaokeEnabled: Boolean = false,
    val crossfadeSeconds: Int = 3,
)

class PlaybackManager(
    private val context: Context,
    private val playerRepository: PlayerRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var crossfadeJob: Job? = null
    private var playRecordedForSongId: String? = null
    private var awaitingInitialReady: Boolean = false
    private var isSeekingInternal: Boolean = false
    private var seekTargetMs: Long = 0L
    private var crossfadeActive: Boolean = false
    private var skipCrossfadeOnce: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    if (awaitingInitialReady && !isSeekingInternal) {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
                Player.STATE_READY -> {
                    awaitingInitialReady = false
                    val finishingSeek = isSeekingInternal
                    if (finishingSeek) {
                        clearSeeking()
                    }
                    controller?.let { c ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                durationMs = c.duration.coerceAtLeast(0L),
                                positionMs = if (finishingSeek) {
                                    seekTargetMs
                                } else {
                                    c.currentPosition.coerceAtLeast(0L)
                                },
                            )
                        }
                    }
                    maybeRecordPlay()
                }
                Player.STATE_ENDED -> handlePlaybackEnded()
                else -> Unit
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (isSeekingInternal &&
                (reason == Player.DISCONTINUITY_REASON_SEEK ||
                    reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT)
            ) {
                clearSeeking()
                _state.update {
                    it.copy(positionMs = newPosition.positionMs.coerceAtLeast(0L))
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSongFromPlayer()
            awaitingInitialReady = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            if (!crossfadeActive) {
                controller?.volume = 1f
            } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
            ) {
                scope.launch { fadeVolume(from = 0f, to = 1f, durationMs = crossfadeMs()) }
            }
            playRecordedForSongId = null
        }

        override fun onPlaybackParametersChanged(
            playbackParameters: androidx.media3.common.PlaybackParameters,
        ) {
            _state.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }
    }

    init {
        scope.launch {
            settingsRepository.crossfadeSecondsFlow.collect { seconds ->
                _state.update { it.copy(crossfadeSeconds = seconds) }
            }
        }
        scope.launch {
            val eq = settingsRepository.getEqualizerSettings()
            EqualizerController.updateSettings(eq)
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
                val mappedRepeat = when (c.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    else -> RepeatMode.ALL
                }
                // Never leave the player in one-and-done OFF mode.
                if (c.repeatMode == Player.REPEAT_MODE_OFF) {
                    c.repeatMode = Player.REPEAT_MODE_ALL
                }
                _state.update {
                    it.copy(
                        isConnected = true,
                        playbackSpeed = c.playbackParameters.speed,
                        shuffleEnabled = c.shuffleModeEnabled,
                        repeatMode = mappedRepeat,
                    )
                }
                syncFromPlayer(c)
                if (c.isPlaying) startProgressUpdates()
            } catch (_: Exception) {
            }
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        stopProgressUpdates()
        crossfadeJob?.cancel()
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
            cancelCrossfade(resetVolume = true)
            val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            playRecordedForSongId = null
            awaitingInitialReady = true
            isSeekingInternal = false
            c.volume = 1f
            c.setMediaItems(queue.map { buildMediaItem(it) }, startIndex, 0L)
            applyRepeatMode(_state.value.repeatMode)
            c.prepare()
            c.play()
            _state.update {
                it.copy(
                    currentSong = song,
                    queue = queue,
                    isLoading = true,
                    isSeeking = false,
                    positionMs = 0L,
                )
            }
        }
    }

    fun preparePaused(song: Song, queue: List<Song> = listOf(song)) {
        scope.launch {
            connectAndAwait()
            val c = controller ?: return@launch
            cancelCrossfade(resetVolume = true)
            val startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            playRecordedForSongId = null
            awaitingInitialReady = true
            c.volume = 1f
            c.setMediaItems(queue.map { buildMediaItem(it) }, startIndex, 0L)
            applyRepeatMode(_state.value.repeatMode)
            c.prepare()
            c.pause()
            _state.update {
                it.copy(currentSong = song, queue = queue, isPlaying = false, isLoading = true)
            }
        }
    }

    fun resume() {
        controller?.play()
    }

    fun pause() {
        cancelCrossfade(resetVolume = true)
        controller?.pause()
    }

    fun playSongId(songId: String, queue: List<Song> = emptyList()) {
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
        if (c.isPlaying) {
            cancelCrossfade(resetVolume = true)
            c.pause()
        } else {
            c.play()
        }
    }

    fun skipNext() {
        skipCrossfadeOnce = true
        cancelCrossfade(resetVolume = true)
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        skipCrossfadeOnce = true
        cancelCrossfade(resetVolume = true)
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        val c = controller ?: return
        val target = positionMs.coerceAtLeast(0L)
        // Seeking near the end should not trigger an in-progress crossfade mid-scrub.
        cancelCrossfade(resetVolume = true)
        isSeekingInternal = true
        seekTargetMs = target
        _state.update {
            it.copy(
                positionMs = target,
                isSeeking = true,
                isLoading = false,
            )
        }
        c.seekTo(target)
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

    fun setKaraoke(enabled: Boolean) {
        _state.update { it.copy(karaokeEnabled = enabled) }
        scope.launch {
            connectAndAwait()
            val c = controller ?: return@launch
            val args = android.os.Bundle().apply {
                putBoolean(MelonetPlaybackService.KEY_KARAOKE_ENABLED, enabled)
            }
            c.sendCustomCommand(
                androidx.media3.session.SessionCommand(
                    MelonetPlaybackService.COMMAND_SET_KARAOKE,
                    android.os.Bundle.EMPTY,
                ),
                args,
            )
        }
    }

    /**
     * When false, ExoPlayer keeps playing even if mic recording requests audio focus.
     * Restore to true when leaving karaoke record mode.
     */
    fun setHandleAudioFocus(handle: Boolean) {
        scope.launch {
            connectAndAwait()
            val c = controller ?: return@launch
            val attrs = androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            c.setAudioAttributes(attrs, handle)
        }
    }

    fun toggleShuffle() {
        val c = controller ?: return
        val enabled = !c.shuffleModeEnabled
        c.shuffleModeEnabled = enabled
        _state.update { it.copy(shuffleEnabled = enabled) }
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.ALL, RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
        }
        applyRepeatMode(next)
    }

    private fun applyRepeatMode(mode: RepeatMode) {
        val normalized = if (mode == RepeatMode.OFF) RepeatMode.ALL else mode
        val c = controller ?: run {
            _state.update { it.copy(repeatMode = normalized) }
            return
        }
        c.repeatMode = when (normalized) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL, RepeatMode.OFF -> Player.REPEAT_MODE_ALL
        }
        _state.update { it.copy(repeatMode = normalized) }
    }

    private fun handlePlaybackEnded() {
        val c = controller ?: return
        if (_state.value.repeatMode == RepeatMode.ONE) return
        if (_state.value.repeatMode == RepeatMode.OFF &&
            !c.hasNextMediaItem() &&
            c.currentMediaItemIndex >= c.mediaItemCount - 1
        ) {
            cancelCrossfade(resetVolume = true)
            c.pause()
            c.seekTo(0)
            _state.update { it.copy(isPlaying = false, positionMs = 0L) }
        }
    }

    private suspend fun buildMediaItem(song: Song): MediaItem {
        val uri = playerRepository.resolveAudioUri(song)
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
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
                isLoading = c.playbackState == Player.STATE_BUFFERING && awaitingInitialReady,
                positionMs = c.currentPosition.coerceAtLeast(0L),
                durationMs = c.duration.coerceAtLeast(0L),
            )
        }
    }

    private fun updateCurrentSongFromPlayer() {
        val songId = controller?.currentMediaItem?.mediaId ?: return
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
                    if (!isSeekingInternal) {
                        val position = c.currentPosition.coerceAtLeast(0L)
                        val duration = c.duration.coerceAtLeast(0L)
                        _state.update {
                            it.copy(positionMs = position, durationMs = duration)
                        }
                        maybeStartCrossfade(position, duration)
                    }
                }
                delay(200L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
    }

    private fun maybeStartCrossfade(positionMs: Long, durationMs: Long) {
        if (skipCrossfadeOnce) {
            skipCrossfadeOnce = false
            return
        }
        val fadeMs = crossfadeMs()
        if (fadeMs <= 0L || crossfadeActive || isSeekingInternal) return
        if (durationMs <= 0L || positionMs <= 0L) return
        val remaining = durationMs - positionMs
        if (remaining > fadeMs || remaining <= 0L) return

        val c = controller ?: return
        if (_state.value.repeatMode == RepeatMode.ONE) return
        if (!c.hasNextMediaItem() && _state.value.repeatMode == RepeatMode.OFF) return

        crossfadeActive = true
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            fadeVolume(from = c.volume, to = 0f, durationMs = remaining.coerceAtMost(fadeMs))
            if (c.hasNextMediaItem() || _state.value.repeatMode == RepeatMode.ALL) {
                c.volume = 0f
                c.seekToNextMediaItem()
                fadeVolume(from = 0f, to = 1f, durationMs = fadeMs)
            } else {
                c.volume = 1f
            }
            crossfadeActive = false
        }
    }

    private suspend fun fadeVolume(from: Float, to: Float, durationMs: Long) {
        val c = controller ?: return
        if (durationMs <= 0L) {
            c.volume = to
            return
        }
        val steps = (durationMs / 40L).toInt().coerceIn(4, 80)
        val stepDelay = durationMs / steps
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            c.volume = from + (to - from) * t
            delay(stepDelay)
        }
        c.volume = to
    }

    private fun cancelCrossfade(resetVolume: Boolean) {
        crossfadeJob?.cancel()
        crossfadeJob = null
        crossfadeActive = false
        if (resetVolume) {
            controller?.volume = 1f
        }
    }

    private fun crossfadeMs(): Long = _state.value.crossfadeSeconds.coerceAtLeast(0) * 1000L

    private fun clearSeeking() {
        isSeekingInternal = false
        _state.update { it.copy(isSeeking = false) }
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
