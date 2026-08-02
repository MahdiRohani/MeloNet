package com.melonet.app.feature.karaoke

import androidx.compose.foundation.background
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
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.repository.KaraokeRecording
import com.melonet.app.data.repository.KaraokeRecordingRepository
import com.melonet.app.feature.player.AudioShareHelper
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

    // Two local players, both without audio-focus steal — single mix timeline.
    val instrumentalPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            .build()
    }
    val vocalPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            .build()
    }

    DisposableEffect(Unit) {
        // Stop the main session so it doesn't compete with the take mix.
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
            instrumentalPlayer.setMediaItem(MediaItem.fromUri(take.instrumentalUrl))
            instrumentalPlayer.prepare()
            instrumentalPlayer.playWhenReady = false

            vocalPlayer.setMediaItem(MediaItem.fromUri(take.vocalPath))
            vocalPlayer.prepare()
            vocalPlayer.playWhenReady = false

            durationMs = (take.durationSec * 1000L).coerceAtLeast(1L)
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

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            val instPos = instrumentalPlayer.currentPosition
            val vocalPos = vocalPlayer.currentPosition
            positionMs = instPos
            // Keep vocal locked to instrumental (±80ms).
            if (kotlin.math.abs(vocalPos - instPos) > 80) {
                vocalPlayer.seekTo(instPos)
            }
            val ended = !instrumentalPlayer.isPlaying &&
                instrumentalPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED
            if (ended) {
                isPlaying = false
                positionMs = durationMs
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
            val target = positionMs.coerceAtMost(durationMs)
            instrumentalPlayer.seekTo(target)
            vocalPlayer.seekTo(target)
            instrumentalPlayer.play()
            vocalPlayer.play()
            isPlaying = true
        }
    }

    fun seekTo(ms: Long) {
        val target = ms.coerceIn(0L, durationMs)
        instrumentalPlayer.seekTo(target)
        vocalPlayer.seekTo(target)
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
                durationMs = durationMs,
                isPlaying = isPlaying,
                onSeek = { seekTo(it) },
                activeColor = scheme.primary,
                trackColor = scheme.onSurface.copy(alpha = 0.2f),
                thumbColor = scheme.primary,
            )

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
