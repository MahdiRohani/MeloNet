package com.melonet.app.feature.karaoke

import android.content.Intent
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.repository.KaraokeRecording
import com.melonet.app.data.repository.KaraokeRecordingRepository
import com.melonet.app.feature.player.PlaybackManager
import com.melonet.app.feature.player.component.PlayerProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

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
    val recording = viewModel.recording
    val playbackManager = viewModel.playbackManager

    LaunchedEffect(recordingId) {
        viewModel.load(recordingId)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    val vocalPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(Unit) {
        onDispose {
            vocalPlayer.release()
            playbackManager.pause()
            playbackManager.setKaraoke(false)
        }
    }

    DisposableEffect(recording) {
        val take = recording
        if (take != null) {
            val instrumentalSong = take.asSong().copy(
                id = take.songId,
                title = take.title,
                audioUrl = take.instrumentalUrl,
                category = "catalog",
            )
            playbackManager.setKaraoke(true)
            playbackManager.preparePaused(instrumentalSong, listOf(instrumentalSong))

            vocalPlayer.setMediaItem(MediaItem.fromUri(take.vocalPath))
            vocalPlayer.prepare()
            vocalPlayer.playWhenReady = false

            durationMs = (take.durationSec * 1000L).coerceAtLeast(1L)
        }
        onDispose {
            vocalPlayer.stop()
            vocalPlayer.clearMediaItems()
            isPlaying = false
            positionMs = 0L
        }
    }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            positionMs = playbackManager.state.value.positionMs
            val vocalPos = vocalPlayer.currentPosition
            // Keep vocal roughly in sync with instrumental (±120ms).
            if (kotlin.math.abs(vocalPos - positionMs) > 120) {
                vocalPlayer.seekTo(positionMs)
            }
            delay(200)
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            playbackManager.pause()
            vocalPlayer.pause()
            isPlaying = false
        } else {
            playbackManager.resume()
            vocalPlayer.play()
            isPlaying = true
        }
    }

    fun seekTo(ms: Long) {
        playbackManager.seekTo(ms)
        vocalPlayer.seekTo(ms)
        positionMs = ms
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val take = recording ?: return@IconButton
                    shareTake(context, take)
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
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp)),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.karaoke_take_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = spacing.xs),
        )

        Spacer(modifier = Modifier.weight(1f))

        PlayerProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeek = { seekTo(it) },
            activeColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            thumbColor = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(spacing.md))

        Box(
            modifier = Modifier
                .size(dimensions.playerPlayButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = { togglePlay() }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(dimensions.iconLg),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

private fun shareTake(context: android.content.Context, recording: KaraokeRecording) {
    val file = File(recording.vocalPath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
            Intent.EXTRA_TEXT,
            context.getString(R.string.karaoke_share_text, recording.title, recording.artistName),
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.player_more_share)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}
