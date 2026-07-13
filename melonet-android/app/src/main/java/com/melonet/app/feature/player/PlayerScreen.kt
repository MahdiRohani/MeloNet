package com.melonet.app.feature.player

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.DownloadStatus
import com.melonet.app.data.model.RepeatMode
import com.melonet.app.feature.player.component.AudioVisualizer
import com.melonet.app.feature.player.component.DynamicPlayerBackground
import com.melonet.app.feature.player.component.PlayerProgressBar
import com.melonet.app.feature.player.component.RotatingCover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    songId: String,
    onNavigateBack: () -> Unit,
    onMinimize: () -> Unit = onNavigateBack,
    onNavigateToArtist: (Int) -> Unit = {},
    onShareToChat: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val context = LocalContext.current
    val imageLoader = remember { ImageLoader(context) }
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    LaunchedEffect(songId) {
        viewModel.playIfNeeded(songId)
    }

    LaunchedEffect(state.currentSong?.coverUrl) {
        val coverUrl = state.currentSong?.coverUrl ?: return@LaunchedEffect
        val colors = extractPaletteColors(context, imageLoader, coverUrl)
        viewModel.handleEvent(PlayerContract.Event.UpdateGradient(colors))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PlayerContract.Effect.NavigateBack -> onNavigateBack()
                is PlayerContract.Effect.NavigateToArtist -> onNavigateToArtist(effect.artistId)
                is PlayerContract.Effect.ShareToChat -> onShareToChat(effect.songId)
                is PlayerContract.Effect.ShareExternal -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                    }
                    context.startActivity(
                        Intent.createChooser(send, context.getString(R.string.player_more_share)),
                    )
                }
                is PlayerContract.Effect.ShowMessage -> {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.player_added_to_playlist, effect.message),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    DynamicPlayerBackground(gradientColors = state.gradientColors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: back + optional sleep timer chip + minimize.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                        tint = onPrimary,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (state.sleepTimerMinutesLeft != null) {
                    Text(
                        text = stringResource(
                            R.string.player_sleep_timer_active,
                            state.sleepTimerMinutesLeft!!,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = onPrimary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                IconButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.Default.CloseFullscreen,
                        contentDescription = stringResource(R.string.cd_minimize),
                        tint = onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            RotatingCover(
                coverUrl = state.currentSong?.coverUrl,
                title = state.currentSong?.title.orEmpty(),
                isPlaying = state.isPlaying,
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            AudioVisualizer(isPlaying = state.isPlaying)

            Spacer(modifier = Modifier.height(spacing.lg))

            // Title / artist with like + share actions.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentSong?.title.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = onPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.currentSong?.artistName.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = onPrimary.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CircleActionButton(
                    icon = Icons.Default.Share,
                    contentDescription = stringResource(R.string.cd_share),
                    tint = onPrimary,
                    onClick = { viewModel.handleEvent(PlayerContract.Event.ShowShareSheet) },
                )
                Spacer(modifier = Modifier.size(spacing.sm))
                CircleActionButton(
                    icon = if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (state.isLiked) MaterialTheme.colorScheme.primary else onPrimary,
                    onClick = { viewModel.handleEvent(PlayerContract.Event.ToggleLike) },
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            PlayerProgressBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                isPlaying = state.isPlaying,
                onSeek = { viewModel.handleEvent(PlayerContract.Event.SeekTo(it)) },
                activeColor = MaterialTheme.colorScheme.primary,
                trackColor = onPrimary.copy(alpha = 0.25f),
                thumbColor = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(state.positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = onPrimary.copy(alpha = 0.7f),
                )
                Text(
                    text = formatDuration(state.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = onPrimary.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Main transport controls: repeat | prev | play | next | shuffle.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.CycleRepeatMode) }) {
                    Icon(
                        imageVector = when (state.repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = stringResource(R.string.cd_repeat),
                        tint = if (state.repeatMode != RepeatMode.OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            onPrimary.copy(alpha = 0.6f)
                        },
                    )
                }
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.SkipPrevious) }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.cd_skip_previous),
                        tint = onPrimary,
                        modifier = Modifier.size(dimensions.iconMd),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(dimensions.playerPlayButtonSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(dimensions.iconMd),
                        )
                    } else {
                        IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.TogglePlayPause) }) {
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
                }
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.SkipNext) }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.cd_skip_next),
                        tint = onPrimary,
                        modifier = Modifier.size(dimensions.iconMd),
                    )
                }
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.ToggleShuffle) }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = stringResource(R.string.cd_shuffle),
                        tint = if (state.shuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            onPrimary.copy(alpha = 0.6f)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Utility row: download | sleep timer | speed | minimize | more.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DownloadButton(
                    status = state.downloadStatus,
                    tint = onPrimary,
                    onClick = { viewModel.handleEvent(PlayerContract.Event.DownloadClicked) },
                )
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.ShowSleepTimerDialog) }) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = stringResource(R.string.cd_sleep_timer),
                        tint = if (state.sleepTimerMinutesLeft != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            onPrimary
                        },
                    )
                }
                // Playback speed: tap to advance to the next step.
                Box(
                    modifier = Modifier
                        .size(dimensions.iconLg)
                        .clip(CircleShape)
                        .clickable { viewModel.handleEvent(PlayerContract.Event.CycleSpeed) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.player_speed_format, formatSpeed(state.playbackSpeed)),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.playbackSpeed != 1f) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            onPrimary
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = stringResource(R.string.cd_minimize),
                        tint = onPrimary,
                    )
                }
                IconButton(onClick = { viewModel.handleEvent(PlayerContract.Event.ShowMoreMenu) }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_more),
                        tint = onPrimary,
                    )
                }
            }
        }
    }

    if (state.showMoreMenu) {
        MoreMenuSheet(
            onAddToPlaylist = { viewModel.handleEvent(PlayerContract.Event.ShowAddToPlaylistDialog) },
            onGoToArtist = { viewModel.handleEvent(PlayerContract.Event.GoToArtist) },
            onShare = { viewModel.handleEvent(PlayerContract.Event.ShowShareSheet) },
            onDismiss = { viewModel.handleEvent(PlayerContract.Event.HideMoreMenu) },
        )
    }

    if (state.showShareSheet) {
        ShareSheet(
            onShareChat = { viewModel.handleEvent(PlayerContract.Event.ShareToChat) },
            onShareExternal = { viewModel.handleEvent(PlayerContract.Event.ShareExternal) },
            onDismiss = { viewModel.handleEvent(PlayerContract.Event.HideShareSheet) },
        )
    }

    if (state.showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = state.playlists,
            onSelect = { viewModel.handleEvent(PlayerContract.Event.AddToPlaylist(it)) },
            onDismiss = { viewModel.handleEvent(PlayerContract.Event.HideAddToPlaylistDialog) },
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
private fun CircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
private fun DownloadButton(
    status: DownloadStatus?,
    tint: Color,
    onClick: () -> Unit,
) {
    val dimensions = MeloNetTheme.dimensions
    val isDownloading = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PENDING
    val enabled = status != DownloadStatus.COMPLETED && !isDownloading
    IconButton(onClick = onClick, enabled = enabled) {
        when {
            isDownloading -> CircularProgressIndicator(
                color = tint,
                modifier = Modifier.size(dimensions.iconSm),
                strokeWidth = dimensions.iconSm / 8,
            )
            status == DownloadStatus.COMPLETED -> Icon(
                imageVector = Icons.Default.DownloadDone,
                contentDescription = stringResource(R.string.cd_download_song),
                tint = MaterialTheme.colorScheme.primary,
            )
            else -> Icon(
                imageVector = Icons.Default.Download,
                contentDescription = stringResource(R.string.cd_download_song),
                tint = tint,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenuSheet(
    onAddToPlaylist: () -> Unit,
    onGoToArtist: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            SheetRow(
                icon = Icons.Default.PlaylistAdd,
                label = stringResource(R.string.player_more_add_to_playlist),
                onClick = onAddToPlaylist,
            )
            SheetRow(
                icon = Icons.Default.Person,
                label = stringResource(R.string.player_more_go_to_artist),
                onClick = onGoToArtist,
            )
            SheetRow(
                icon = Icons.Default.Share,
                label = stringResource(R.string.player_more_share),
                onClick = onShare,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(
    onShareChat: () -> Unit,
    onShareExternal: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.player_share_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SheetRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.player_share_chat),
                onClick = onShareChat,
            )
            SheetRow(
                icon = Icons.Default.Share,
                label = stringResource(R.string.player_share_external),
                onClick = onShareExternal,
            )
        }
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(label) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<com.melonet.app.data.model.Playlist>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_add_to_playlist_title)) },
        text = {
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.player_playlists_empty))
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { onSelect(playlist.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = playlist.title,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
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

private fun formatSpeed(speed: Float): String {
    return if (speed % 1f == 0f) {
        speed.toInt().toString()
    } else {
        speed.toString().trimEnd('0').trimEnd('.')
    }
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
