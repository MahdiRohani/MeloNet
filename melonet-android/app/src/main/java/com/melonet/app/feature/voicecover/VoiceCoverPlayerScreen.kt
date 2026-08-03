package com.melonet.app.feature.voicecover

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song
import com.melonet.app.data.model.VoiceCover
import com.melonet.app.feature.player.PlayerContract
import com.melonet.app.feature.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCoverPlayerScreen(
    viewModel: VoiceCoverPlayerViewModel,
    playerViewModel: PlayerViewModel,
    coverId: Long,
    onNavigateBack: () -> Unit,
    onOpenFullPlayer: (Song) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(coverId) {
        viewModel.handleEvent(VoiceCoverPlayerContract.Event.Load(coverId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VoiceCoverPlayerContract.Effect.PlayCover -> {
                    val song = effect.cover.toSong()
                    playerViewModel.handleEvent(PlayerContract.Event.PlaySong(song, listOf(song)))
                }
                VoiceCoverPlayerContract.Effect.Deleted -> onNavigateBack()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.tertiary.copy(alpha = 0.28f),
                        scheme.background,
                        scheme.background,
                    ),
                ),
            ),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.voice_cover_player_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                    )
                }
            },
            actions = {
                if (state.cover?.isReady == true || state.cover?.isFailed == true) {
                    IconButton(
                        onClick = {
                            viewModel.handleEvent(VoiceCoverPlayerContract.Event.Delete)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_voice_cover),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.cover == null -> {
                EmptyState(
                    title = stringResource(
                        when (state.error) {
                            "no_connection" -> R.string.error_no_connection
                            "not_found" -> R.string.error_not_found
                            else -> R.string.voice_cover_load_failed
                        },
                    ),
                    modifier = Modifier.padding(top = spacing.xl),
                )
            }
            state.cover!!.isInProgress || state.isPolling -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.cover!!.sourceTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(spacing.lg))
                    VoiceCoverProgressCard(
                        artistName = state.cover!!.targetArtistName,
                        cover = state.cover,
                    )
                }
            }
            state.cover!!.isFailed || (!state.error.isNullOrBlank() && !state.cover!!.isReady) -> {
                EmptyState(
                    title = stringResource(R.string.voice_cover_create_failed),
                    description = state.cover?.error?.takeIf { it.isNotBlank() }
                        ?: state.error,
                    modifier = Modifier.padding(top = spacing.xl),
                )
            }
            else -> {
                ReadyPlayerContent(
                    cover = state.cover!!,
                    isPlaying = playerState.isPlaying &&
                        playerState.currentSong?.id == state.cover!!.toSong().id,
                    onPlayPause = {
                        val song = state.cover!!.toSong()
                        if (playerState.currentSong?.id == song.id) {
                            playerViewModel.handleEvent(PlayerContract.Event.TogglePlayPause)
                        } else {
                            viewModel.handleEvent(VoiceCoverPlayerContract.Event.Play)
                        }
                    },
                    onOpenFull = {
                        onOpenFullPlayer(state.cover!!.toSong())
                    },
                )
            }
        }
    }
}

@Composable
private fun ReadyPlayerContent(
    cover: VoiceCover,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onOpenFull: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val dimensions = MeloNetTheme.dimensions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(spacing.lg))
        MeloImage(
            imageUrl = cover.coverUrl,
            contentDescription = cover.sourceTitle,
            modifier = Modifier
                .size(dimensions.playerCoverSize)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(spacing.lg))
        Text(
            text = cover.sourceTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = stringResource(R.string.voice_cover_in_voice_of, cover.targetArtistName),
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.md))
        androidx.compose.material3.TextButton(onClick = onOpenFull) {
            Text(stringResource(R.string.voice_cover_open_full_player))
        }
    }
}
