package com.melonet.app.feature.karaoke

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.feature.player.component.PlayerProgressBar

@Composable
fun KaraokePlayerScreen(
    viewModel: KaraokePlayerViewModel,
    songId: String,
    onNavigateBack: () -> Unit,
    onRecordingSaved: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.handleEvent(KaraokePlayerContract.Event.PermissionGranted)
        } else {
            viewModel.handleEvent(KaraokePlayerContract.Event.PermissionDenied)
        }
    }

    LaunchedEffect(songId) {
        viewModel.start(songId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                KaraokePlayerContract.Effect.RequestMicPermission -> {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.handleEvent(KaraokePlayerContract.Event.PermissionGranted)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                is KaraokePlayerContract.Effect.RecordingSaved -> onRecordingSaved(effect.recordingId)
                is KaraokePlayerContract.Effect.ShowMessage -> {
                    val text = when (effect.message) {
                        "record_saved" -> context.getString(R.string.karaoke_record_saved)
                        "record_failed" -> context.getString(R.string.karaoke_record_failed)
                        "mic_permission_denied" -> context.getString(R.string.karaoke_mic_permission)
                        else -> effect.message
                    }
                    android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(state.currentLineIndex) {
        val index = state.currentLineIndex
        if (index >= 0 && state.lyrics.lines.isNotEmpty()) {
            listState.animateScrollToItem((index - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        MeloImage(
            imageUrl = state.song?.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.18f },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.background.copy(alpha = 0.55f),
                            scheme.background.copy(alpha = 0.92f),
                            scheme.background,
                        ),
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.sm, vertical = spacing.sm)
                    .padding(top = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                    )
                }
                MeloImage(
                    imageUrl = state.song?.coverUrl,
                    contentDescription = state.song?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(modifier = Modifier.size(spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.song?.title.orEmpty().ifBlank { "…" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.song?.artistName.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilterChip(
                    selected = state.karaokeEnabled,
                    onClick = { viewModel.handleEvent(KaraokePlayerContract.Event.ToggleVocals) },
                    enabled = state.lyricsReady,
                    label = {
                        Text(
                            stringResource(
                                if (state.karaokeEnabled) R.string.karaoke_mode_instrumental
                                else R.string.karaoke_mode_original,
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }

            if (state.lyrics.synced && state.lyricsReady && !state.lyrics.isEmpty) {
                Surface(
                    color = scheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.padding(horizontal = spacing.md),
                ) {
                    Text(
                        text = stringResource(R.string.karaoke_synced_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(spacing.xs))
            } else if (state.lyricsReady && !state.lyrics.isEmpty && !state.lyrics.synced) {
                Text(
                    text = stringResource(R.string.karaoke_unsynced_warning),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.xs),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.isLoadingLyrics -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(spacing.md))
                            Text(
                                text = stringResource(R.string.karaoke_loading_lyrics),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    state.lyrics.isEmpty -> {
                        Text(
                            text = stringResource(R.string.karaoke_lyrics_not_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(spacing.lg),
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = spacing.lg,
                                vertical = spacing.xxl,
                            ),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            itemsIndexed(state.lyrics.lines) { index, line ->
                                LyricRow(
                                    text = line.text,
                                    isCurrent = index == state.currentLineIndex,
                                    isPast = index < state.currentLineIndex,
                                    onClick = {
                                        viewModel.handleEvent(KaraokePlayerContract.Event.LineClicked(index))
                                    },
                                )
                            }
                        }
                    }
                }

                state.countdownSeconds?.let { sec ->
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(scheme.scrim.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = sec.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
            ) {
                if (state.lyrics.synced && !state.lyrics.isEmpty) {
                    OffsetControls(
                        offsetMs = state.lyricsOffsetMs,
                        onEarlier = { viewModel.handleEvent(KaraokePlayerContract.Event.NudgeOffsetEarlier) },
                        onLater = { viewModel.handleEvent(KaraokePlayerContract.Event.NudgeOffsetLater) },
                        onReset = { viewModel.handleEvent(KaraokePlayerContract.Event.ResetOffset) },
                    )
                    Spacer(modifier = Modifier.height(spacing.sm))
                }

                if (state.isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.sm),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.size(spacing.xs))
                        Text(
                            text = stringResource(
                                R.string.karaoke_recording,
                                formatSeconds(state.recordingSeconds),
                            ),
                            color = Color.Red,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                PlayerProgressBar(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    onSeek = { viewModel.handleEvent(KaraokePlayerContract.Event.SeekTo(it)) },
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
                        text = formatDuration(state.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(state.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (state.isRecording) {
                                viewModel.handleEvent(KaraokePlayerContract.Event.StopRecording)
                            } else {
                                viewModel.handleEvent(KaraokePlayerContract.Event.StartRecording)
                            }
                        },
                        enabled = state.lyricsReady && state.countdownSeconds == null,
                    ) {
                        Icon(
                            imageVector = if (state.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = stringResource(R.string.karaoke_record),
                            tint = if (state.isRecording) Color.Red else scheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(dimensions.playerPlayButtonSize)
                            .clip(CircleShape)
                            .background(scheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = { viewModel.handleEvent(KaraokePlayerContract.Event.TogglePlayPause) },
                            enabled = state.lyricsReady && state.countdownSeconds == null,
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    if (state.isPlaying) R.string.cd_pause else R.string.cd_play,
                                ),
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(dimensions.iconLg),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

@Composable
private fun OffsetControls(
    offsetMs: Long,
    onEarlier: () -> Unit,
    onLater: () -> Unit,
    onReset: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val label = when {
        offsetMs == 0L -> "0.0s"
        offsetMs > 0 -> "+%.1fs".format(offsetMs / 1000f)
        else -> "%.1fs".format(offsetMs / 1000f)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onEarlier) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.karaoke_offset_earlier))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.karaoke_offset_label, label),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            if (offsetMs != 0L) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.karaoke_offset_reset), fontSize = 12.sp)
                }
            }
        }
        IconButton(onClick = onLater) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.karaoke_offset_later))
        }
    }
}

@Composable
private fun LyricRow(
    text: String,
    isCurrent: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = when {
            isCurrent -> MaterialTheme.colorScheme.primary
            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        },
        label = "lyric_color",
    )
    val scale by animateFloatAsState(targetValue = if (isCurrent) 1.1f else 1f, label = "lyric_scale")

    Text(
        text = text,
        style = if (isCurrent) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

private fun formatSeconds(total: Int): String {
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
