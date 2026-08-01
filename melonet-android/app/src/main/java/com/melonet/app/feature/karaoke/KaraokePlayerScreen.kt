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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.melonet.app.R
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.song?.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.song?.artistName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilterChip(
                selected = state.karaokeEnabled,
                onClick = { viewModel.handleEvent(KaraokePlayerContract.Event.ToggleVocals) },
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.lyrics.isEmpty -> {
                    Text(
                        text = stringResource(R.string.karaoke_lyrics_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(spacing.lg),
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.xxl),
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
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        ) {
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
                activeColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                thumbColor = MaterialTheme.colorScheme.primary,
            )
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
                    enabled = state.lyricsReady,
                ) {
                    Icon(
                        imageVector = if (state.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = stringResource(R.string.karaoke_record),
                        tint = if (state.isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(dimensions.playerPlayButtonSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = { viewModel.handleEvent(KaraokePlayerContract.Event.TogglePlayPause) },
                        enabled = state.lyricsReady,
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(
                                if (state.isPlaying) R.string.cd_pause else R.string.cd_play,
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(dimensions.iconLg),
                        )
                    }
                }

                // Spacer symmetry for mic button
                Spacer(modifier = Modifier.size(48.dp))
            }
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
    val scale by animateFloatAsState(targetValue = if (isCurrent) 1.08f else 1f, label = "lyric_scale")

    Text(
        text = text,
        style = if (isCurrent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
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
            .padding(vertical = 2.dp),
    )
}

private fun formatSeconds(total: Int): String {
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
