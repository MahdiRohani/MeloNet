package com.melonet.app.feature.voicecover

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.MeloSearchBar
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song
import com.melonet.app.data.model.VoiceCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCoverScreen(
    viewModel: VoiceCoverViewModel,
    onNavigateBack: () -> Unit,
    onSongSelected: (String) -> Unit,
    onCoverSelected: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val showingSearch = state.query.isNotBlank() || state.hasSearched

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VoiceCoverContract.Effect.OpenCreate -> onSongSelected(effect.songId)
                is VoiceCoverContract.Effect.OpenPlayer -> onCoverSelected(effect.coverId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        VoiceCoverHubHeader(
            onNavigateBack = onNavigateBack,
            query = state.query,
            onQueryChange = { viewModel.handleEvent(VoiceCoverContract.Event.QueryChanged(it)) },
            onSearch = { viewModel.handleEvent(VoiceCoverContract.Event.Submit) },
        )

        VoiceCoverHubBody(
            showingSearch = showingSearch,
            state = state,
            onSongSelected = { viewModel.handleEvent(VoiceCoverContract.Event.SongClicked(it)) },
            onCoverSelected = { viewModel.handleEvent(VoiceCoverContract.Event.CoverClicked(it)) },
            onCoverDelete = { viewModel.handleEvent(VoiceCoverContract.Event.CoverDelete(it)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceCoverHubHeader(
    onNavigateBack: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.tertiary.copy(alpha = 0.22f),
                        scheme.primary.copy(alpha = 0.1f),
                        scheme.background,
                    ),
                ),
            ),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.voice_cover_banner_title),
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
        Text(
            text = stringResource(R.string.voice_cover_banner_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.md),
        )
        Spacer(modifier = Modifier.height(spacing.md))
        MeloSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.voice_cover_search_hint),
            onSearch = { onSearch() },
        )
        Spacer(modifier = Modifier.height(spacing.sm))
    }
}

@Composable
private fun VoiceCoverHubBody(
    showingSearch: Boolean,
    state: VoiceCoverContract.State,
    onSongSelected: (Song) -> Unit,
    onCoverSelected: (VoiceCover) -> Unit,
    onCoverDelete: (VoiceCover) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    when {
        showingSearch && state.isSearching -> LoadingCentered()
        showingSearch && !state.searchError.isNullOrBlank() -> {
            EmptyState(
                title = stringResource(
                    when (state.searchError) {
                        "rate_limited" -> R.string.error_rate_limited
                        "no_connection" -> R.string.error_no_connection
                        "timeout" -> R.string.error_timeout
                        else -> R.string.voice_cover_search_failed
                    },
                ),
                modifier = Modifier.padding(top = spacing.xl),
            )
        }
        showingSearch && state.results.isEmpty() && state.hasSearched -> {
            EmptyState(
                title = stringResource(R.string.voice_cover_no_results),
                modifier = Modifier.padding(top = spacing.xl),
            )
        }
        showingSearch -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(state.results, key = { it.id }) { song ->
                    VoiceCoverSongRow(song = song, onClick = { onSongSelected(song) })
                }
            }
        }
        state.isLoadingCatalog -> LoadingCentered()
        !state.catalogError.isNullOrBlank() && state.readyCovers.isEmpty() -> {
            EmptyState(
                title = stringResource(R.string.voice_cover_catalog_failed),
                description = stringResource(R.string.voice_cover_empty_description),
                icon = Icons.Default.GraphicEq,
                modifier = Modifier.padding(top = spacing.xl),
            )
        }
        state.readyCovers.isEmpty() -> {
            EmptyState(
                title = stringResource(R.string.voice_cover_empty_title),
                description = stringResource(R.string.voice_cover_empty_description),
                icon = Icons.Default.GraphicEq,
                modifier = Modifier.padding(top = spacing.xl),
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                item {
                    Column(modifier = Modifier.padding(vertical = spacing.sm)) {
                        Text(
                            text = stringResource(R.string.voice_cover_ready_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.voice_cover_ready_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.readyCovers, key = { it.id }) { cover ->
                    VoiceCoverCatalogRow(
                        cover = cover,
                        onClick = { onCoverSelected(cover) },
                        onDelete = { onCoverDelete(cover) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingCentered() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun VoiceCoverSongRow(song: Song, onClick: () -> Unit) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        MeloImage(
            imageUrl = song.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceCoverCatalogRow(
    cover: VoiceCover,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.errorContainer),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_voice_cover),
                    tint = scheme.onErrorContainer,
                    modifier = Modifier.padding(spacing.md),
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.background)
                .clickable(onClick = onClick)
                .padding(vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box {
                MeloImage(
                    imageUrl = cover.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(scheme.tertiary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = scheme.onTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cover.sourceTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.voice_cover_in_voice_of,
                        cover.targetArtistName,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_voice_cover),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
