package com.melonet.app.feature.karaoke

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.network.MediaUrl
import com.melonet.app.data.repository.KaraokeRecording
import com.melonet.app.data.repository.KaraokeRecordingRepository
import com.melonet.app.feature.player.AudioShareHelper
import com.melonet.app.feature.player.KaraokeExoPlayerFactory
import com.melonet.app.feature.player.PlaybackManager
import com.melonet.app.feature.player.component.PlayerProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class KaraokeTakePlayerViewModel(
    private val repository: KaraokeRecordingRepository,
    val playbackManager: PlaybackManager,
) : ViewModel() {
    var recording by mutableStateOf<KaraokeRecording?>(null)
        private set

    fun load(id: Long) {
        viewModelScope.launch {
            recording = repository.getById(id)
        }
    }
}

@Composable
fun KaraokeTakePlayerScreen(
    recordingId: Long,
    viewModel: KaraokeTakePlayerViewModel,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val scheme = MaterialTheme.colorScheme
    val recording = viewModel.recording
    val playbackManager = viewModel.playbackManager
    val scope = rememberCoroutineScope()
    val audioShareHelper = koinInject<AudioShareHelper>()

    LaunchedEffect(recordingId) {
        viewModel.load(recordingId)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Instrumental player applies the same L−R vocal cancel used during recording.
    // Must follow HTTP→HTTPS 302 redirects from /api/stream to the CDN.
    val instrumentalPair = remember {
        KaraokeExoPlayerFactory.createInstrumentalPlayer(context, handleAudioFocus = false)
    }
    val instrumentalPlayer = instrumentalPair.first
    val vocalPlayer = remember {
        KaraokeExoPlayerFactory.createVocalPlayer(context, handleAudioFocus = false)
    }

    DisposableEffect(Unit) {
        playbackManager.pause()
        playbackManager.setKaraoke(false)
        onDispose {
            instrumentalPlayer.release()
            vocalPlayer.release()
            isPlaying = false
        }
    }

    DisposableEffect(recording) {
        val take = recording
        if (take != null) {
            val instrumentalUri = MediaUrl.resolve(take.instrumentalUrl) ?: take.instrumentalUrl
            instrumentalPlayer.setMediaItem(MediaItem.fromUri(instrumentalUri))
            instrumentalPlayer.prepare()
            instrumentalPlayer.playWhenReady = false

            val vocalUri = MediaUrl.resolve(take.vocalPath) ?: take.vocalPath
            vocalPlayer.setMediaItem(MediaItem.fromUri(vocalUri))
            vocalPlayer.prepare()
            vocalPlayer.playWhenReady = false

            durationMs = (take.durationSec * 1000L).coerceAtLeast(1L)
            positionMs = 0L
        }
        onDispose {
            instrumentalPlayer.stop()
            instrumentalPlayer.clearMediaItems()
            vocalPlayer.stop()
            vocalPlayer.clearMediaItems()
            isPlaying = false
            positionMs = 0L
        }
    }

    LaunchedEffect(isPlaying, recording) {
        while (isActive && isPlaying) {
            val instrumentalOk = instrumentalPlayer.playbackState != Player.STATE_IDLE &&
                instrumentalPlayer.playerError == null
            val vocalOk = vocalPlayer.playbackState != Player.STATE_IDLE &&
                vocalPlayer.playerError == null

            val vocalDur = vocalPlayer.duration
            if (vocalDur > 0L && vocalDur != C.TIME_UNSET) {
                durationMs = vocalDur
            }

            val masterPos = when {
                instrumentalOk && instrumentalPlayer.duration > 0 ->
                    instrumentalPlayer.currentPosition
                else -> vocalPlayer.currentPosition
            }.coerceAtLeast(0L)
            positionMs = masterPos.coerceAtMost(durationMs)

            // Keep vocal locked to instrumental only while both are healthy.
            if (instrumentalOk && vocalOk) {
                val vocalPos = vocalPlayer.currentPosition
                if (kotlin.math.abs(vocalPos - masterPos) > 80) {
                    vocalPlayer.seekTo(masterPos)
                }
            }

            val instrumentalEnded = instrumentalOk &&
                !instrumentalPlayer.isPlaying &&
                instrumentalPlayer.playbackState == Player.STATE_ENDED
            val vocalEnded = vocalOk &&
                !vocalPlayer.isPlaying &&
                vocalPlayer.playbackState == Player.STATE_ENDED
            val reachedEnd = positionMs >= durationMs - 120 || instrumentalEnded || vocalEnded

            if (reachedEnd) {
                instrumentalPlayer.pause()
                vocalPlayer.pause()
                isPlaying = false
                positionMs = durationMs
                break
            }
            delay(100)
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            instrumentalPlayer.pause()
            vocalPlayer.pause()
            isPlaying = false
        } else {
            val target = positionMs.coerceIn(0L, durationMs)
            val restart = target >= durationMs - 80
            val seekTarget = if (restart) 0L else target
            if (restart) positionMs = 0L
            if (instrumentalPlayer.playerError == null) {
                instrumentalPlayer.seekTo(seekTarget)
                instrumentalPlayer.play()
            }
            if (vocalPlayer.playerError == null) {
                vocalPlayer.seekTo(seekTarget)
                vocalPlayer.play()
            }
            isPlaying = true
        }
    }

    fun seekTo(ms: Long) {
        val target = ms.coerceIn(0L, durationMs)
        if (instrumentalPlayer.playerError == null) instrumentalPlayer.seekTo(target)
        if (vocalPlayer.playerError == null) vocalPlayer.seekTo(target)
        positionMs = target
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        MeloImage(
            imageUrl = recording?.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.background.copy(alpha = 0.45f),
                            scheme.background.copy(alpha = 0.85f),
                            scheme.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text(
                    text = stringResource(R.string.karaoke_my_take),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        val take = recording ?: return@IconButton
                        scope.launch {
                            val payload = audioShareHelper.prepare(take.asSong()) ?: return@launch
                            audioShareHelper.launchShareChooser(
                                context,
                                payload,
                                context.getString(R.string.player_more_share),
                            )
                        }
                    },
                ) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share))
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            MeloImage(
                imageUrl = recording?.coverUrl,
                contentDescription = recording?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = recording?.title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = recording?.artistName.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.karaoke_take_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.primary,
                modifier = Modifier.padding(top = spacing.xs),
            )

            Spacer(modifier = Modifier.weight(1f))

            PlayerProgressBar(
                positionMs = positionMs,
                durationMs = durationMs.coerceAtLeast(1L),
                isPlaying = isPlaying,
                onSeek = { seekTo(it) },
                activeColor = scheme.primary,
                trackColor = scheme.onSurface.copy(alpha = 0.2f),
                thumbColor = scheme.primary,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTakeDuration(positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = formatTakeDuration(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            Box(
                modifier = Modifier
                    .size(dimensions.playerPlayButtonSize)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { togglePlay() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(dimensions.iconLg),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}

private fun formatTakeDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
