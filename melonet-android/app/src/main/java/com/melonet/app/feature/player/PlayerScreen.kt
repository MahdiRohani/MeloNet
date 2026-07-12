package com.melonet.app.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.DownloadStatus
import com.melonet.app.feature.player.component.AudioVisualizer
import com.melonet.app.feature.player.component.DynamicPlayerBackground
import com.melonet.app.feature.player.component.RotatingCover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    songId: String,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val context = LocalContext.current
    val imageLoader = remember { ImageLoader(context) }

    LaunchedEffect(songId) {
        viewModel.playIfNeeded(songId)
    }

    LaunchedEffect(state.currentSong?.coverUrl) {
        val coverUrl = state.currentSong?.coverUrl ?: return@LaunchedEffect
        val colors = extractPaletteColors(context, imageLoader, coverUrl)
        viewModel.handleEvent(PlayerContract.Event.UpdateGradient(colors))
    }

    DynamicPlayerBackground(gradientColors = state.gradientColors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                if (state.sleepTimerMinutesLeft != null) {
                    Text(
                        text = stringResource(
                            R.string.player_sleep_timer_active,
                            state.sleepTimerMinutesLeft!!,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.ShowSleepTimerDialog) }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = stringResource(R.string.cd_sleep_timer),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    val isDownloading = state.downloadStatus == DownloadStatus.DOWNLOADING ||
                        state.downloadStatus == DownloadStatus.PENDING
                    val downloadEnabled = state.downloadStatus != DownloadStatus.COMPLETED && !isDownloading
                    IconButton(
                        onClick = { viewModel.handleEvent(PlayerContract.Event.DownloadClicked) },
                        enabled = downloadEnabled,
                    ) {
                        when {
                            isDownloading -> {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(dimensions.iconSm),
                                    strokeWidth = dimensions.iconSm / 8,
                                )
                            }
                            state.downloadStatus == DownloadStatus.COMPLETED -> {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = stringResource(R.string.cd_download_song),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = stringResource(R.string.cd_download_song),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            RotatingCover(
                coverUrl = state.currentSong?.coverUrl,
                title = state.currentSong?.title.orEmpty(),
                isPlaying = state.isPlaying,
                modifier = Modifier,
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            AudioVisualizer(
                isPlaying = state.isPlaying,
                modifier = Modifier.padding(horizontal = spacing.md),
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = state.currentSong?.title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.currentSong?.artistName.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            val duration = state.durationMs.coerceAtLeast(1L)
            Slider(
                value = state.positionMs.toFloat().coerceIn(0f, duration.toFloat()),
                onValueChange = { viewModel.handleEvent(PlayerContract.Event.SeekTo(it.toLong())) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(state.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )
                Text(
                    text = formatDuration(state.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.ShowSpeedDialog) }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.cd_playback_speed),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.SkipPrevious) }) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = stringResource(R.string.cd_skip_previous),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(dimensions.iconMd),
                    )
                }
                IconButton(
                    onClick = { viewModel.handleEvent(PlayerContract.Event.TogglePlayPause) },
                    modifier = Modifier.size(dimensions.playerPlayButtonSize),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(dimensions.iconMd),
                        )
                    } else {
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
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.SkipNext) }) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = stringResource(R.string.cd_skip_next),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(dimensions.iconMd),
                    )
                }
                Text(
                    text = stringResource(R.string.player_speed_format, state.playbackSpeed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }

    if (state.showSpeedDialog) {
        SpeedDialog(
            currentSpeed = state.playbackSpeed,
            onSpeedSelected = { viewModel.handleEvent(PlayerContract.Event.SetSpeed(it)) },
            onDismiss = { viewModel.handleEvent(PlayerContract.Event.HideSpeedDialog) },
        )
    }

    if (state.showSleepTimerDialog) {
        SleepTimerDialog(
            onMinutesSelected = { viewModel.handleEvent(PlayerContract.Event.SetSleepTimer(it)) },
            onDismiss = { viewModel.handleEvent(PlayerContract.Event.HideSleepTimerDialog) },
        )
    }

    if (state.showUpgradeDialog) {
        UpgradeDialog(
            onUpgrade = { viewModel.upgradePremium() },
            onDismiss = { viewModel.handleEvent(PlayerContract.Event.DismissUpgradeDialog) },
        )
    }
}

@Composable
private fun SpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_speed_title)) },
        text = {
            Column {
                speeds.forEach { speed ->
                    TextButton(
                        onClick = { onSpeedSelected(speed) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.player_speed_format, speed),
                            color = if (speed == currentSpeed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun SleepTimerDialog(
    onMinutesSelected: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(null, 5, 15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_sleep_timer_title)) },
        text = {
            Column {
                options.forEach { minutes ->
                    TextButton(
                        onClick = {
                            onMinutesSelected(minutes)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = when (minutes) {
                                null -> stringResource(R.string.player_sleep_timer_off)
                                else -> stringResource(R.string.player_sleep_timer_minutes, minutes)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun UpgradeDialog(
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.premium_upgrade_title)) },
        text = { Text(stringResource(R.string.premium_upgrade_description)) },
        confirmButton = {
            TextButton(onClick = onUpgrade) {
                Text(stringResource(R.string.premium_upgrade_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private suspend fun extractPaletteColors(
    context: android.content.Context,
    imageLoader: ImageLoader,
    url: String,
): List<Long> {
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(128)
                .build()
            val result = imageLoader.execute(request)
            if (result !is SuccessResult) return@withContext emptyList()
            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: return@withContext emptyList()
            val palette = Palette.from(bitmap).generate()
            listOfNotNull(
                palette.getDarkVibrantColor(0xFF1A1A2E.toInt()),
                palette.getVibrantColor(0xFF16213E.toInt()),
                palette.getMutedColor(0xFF0F3460.toInt()),
            ).map { it.toLong() and 0xFFFFFFFFL }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
