package com.melonet.app.feature.voicecover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.MeloImageFallback
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song
import com.melonet.app.data.model.VoiceArtist
import com.melonet.app.data.model.VoiceCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCoverCreateScreen(
    viewModel: VoiceCoverCreateViewModel,
    songId: String,
    onNavigateBack: () -> Unit,
    onOpenPlayer: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(songId) {
        viewModel.handleEvent(VoiceCoverCreateContract.Event.Load(songId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VoiceCoverCreateContract.Effect.OpenPlayer -> onOpenPlayer(effect.coverId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.voice_cover_choose_artist),
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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.song == null || !state.error.isNullOrBlank() && state.artists.isEmpty() -> {
                EmptyState(
                    title = stringResource(
                        when (state.error) {
                            "no_connection" -> R.string.error_no_connection
                            "timeout" -> R.string.error_timeout
                            "not_found" -> R.string.error_not_found
                            else -> R.string.voice_cover_load_failed
                        },
                    ),
                    modifier = Modifier.padding(top = spacing.xl),
                )
            }
            else -> {
                SongSummary(song = state.song!!)
                Spacer(modifier = Modifier.height(spacing.md))

                if (state.isProcessing || state.isSubmitting) {
                    VoiceCoverProgressCard(
                        artistName = state.artists
                            .firstOrNull { it.slug == state.selectedSlug }
                            ?.displayName
                            .orEmpty(),
                        cover = state.activeCover,
                        modifier = Modifier.padding(horizontal = spacing.md),
                    )
                    Spacer(modifier = Modifier.height(spacing.md))
                }

                if (!state.error.isNullOrBlank() && !state.isProcessing) {
                    Text(
                        text = stringResource(
                            when (state.error) {
                                "no_connection" -> R.string.error_no_connection
                                "timeout" -> R.string.error_timeout
                                "model_unavailable" -> R.string.voice_cover_model_unavailable
                                else -> R.string.voice_cover_create_failed
                            },
                        ),
                        color = scheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                    )
                }

                Text(
                    text = stringResource(R.string.voice_cover_pick_voice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.artists, key = { it.id }) { artist ->
                        ArtistPickCard(
                            artist = artist,
                            selected = state.selectedSlug == artist.slug,
                            enabled = !state.isSubmitting && !state.isProcessing,
                            onClick = {
                                viewModel.handleEvent(
                                    VoiceCoverCreateContract.Event.ArtistSelected(artist.slug),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongSummary(song: Song) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        MeloImage(
            imageUrl = song.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun VoiceCoverProgressCard(
    artistName: String,
    cover: VoiceCover?,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val pct = (cover?.progressPct ?: 0).coerceIn(0, 100)
    val progress by animateFloatAsState(
        targetValue = (pct / 100f).coerceIn(0.02f, 1f),
        label = "voiceCoverProgress",
    )
    val stageLabel = progressStageLabel(cover?.progressStage.orEmpty())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(96.dp),
                strokeWidth = 8.dp,
                strokeCap = StrokeCap.Round,
                trackColor = scheme.surface.copy(alpha = 0.55f),
            )
            Text(
                text = stringResource(R.string.voice_cover_progress_pct, pct),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.voice_cover_building),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (artistName.isBlank()) {
                stringResource(R.string.voice_cover_building_hint)
            } else {
                stringResource(R.string.voice_cover_building_for, artistName)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stageLabel,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun progressStageLabel(stage: String): String = when (stage) {
    "queued", "downloading", "preparing" -> stringResource(R.string.voice_cover_stage_preparing)
    "separating" -> stringResource(R.string.voice_cover_stage_separating)
    "converting" -> stringResource(R.string.voice_cover_stage_converting)
    "mixing", "encoding" -> stringResource(R.string.voice_cover_stage_mixing)
    "uploading" -> stringResource(R.string.voice_cover_stage_uploading)
    "done" -> stringResource(R.string.voice_cover_stage_done)
    else -> stringResource(R.string.voice_cover_building_hint)
}

@Composable
private fun ArtistPickCard(
    artist: VoiceArtist,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val borderColor = if (selected) scheme.primary else Color.Transparent
    val context = LocalContext.current
    val avatarUrl = VoiceArtistAvatars.resolve(context, artist.slug, artist.avatarUrl)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.4f))
            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = spacing.md, horizontal = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center,
        ) {
            MeloImage(
                imageUrl = avatarUrl,
                contentDescription = artist.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 3.dp else 0.dp,
                        color = if (selected) scheme.primary else Color.Transparent,
                        shape = CircleShape,
                    ),
                contentScale = ContentScale.Crop,
                targetSize = 88.dp,
                fallback = MeloImageFallback.Person,
                crossfade = true,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(scheme.surface),
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.sm))
        Text(
            text = artist.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
