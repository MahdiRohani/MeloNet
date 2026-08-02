package com.melonet.app.feature.karaoke

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeScreen(
    viewModel: KaraokeViewModel,
    onNavigateBack: () -> Unit,
    onSongSelected: (String) -> Unit,
    onOpenMyTakes: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val showingSearch = state.query.isNotBlank() || state.hasSearched

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        KaraokeHubHeader(
            onNavigateBack = onNavigateBack,
            onOpenMyTakes = onOpenMyTakes,
            query = state.query,
            onQueryChange = { viewModel.handleEvent(KaraokeContract.Event.QueryChanged(it)) },
            onSearch = { viewModel.handleEvent(KaraokeContract.Event.Submit) },
        )

        KaraokeHubBody(
            showingSearch = showingSearch,
            state = state,
            onSongSelected = onSongSelected,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KaraokeHubHeader(
    onNavigateBack: () -> Unit,
    onOpenMyTakes: () -> Unit,
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
                        scheme.primary.copy(alpha = 0.22f),
                        scheme.secondary.copy(alpha = 0.1f),
                        scheme.background,
                    ),
                ),
            ),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.karaoke_banner_title),
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
            text = stringResource(R.string.karaoke_banner_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.md),
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.primary.copy(alpha = 0.14f))
                .clickable(onClick = onOpenMyTakes)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = scheme.primary,
            )
            Text(
                text = stringResource(R.string.karaoke_hero_cta),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(spacing.md))
        MeloSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = stringResource(R.string.karaoke_search_hint),
            onSearch = { onSearch() },
        )
        Spacer(modifier = Modifier.height(spacing.sm))
    }
}

@Composable
private fun KaraokeHubBody(
    showingSearch: Boolean,
    state: KaraokeContract.State,
    onSongSelected: (String) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    when {
        showingSearch && state.isSearching -> {
            LoadingCentered()
        }
        showingSearch && state.results.isEmpty() && state.hasSearched -> {
            EmptyState(
                title = stringResource(R.string.karaoke_no_results),
                modifier = Modifier.padding(top = spacing.xl),
            )
        }
        showingSearch -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = spacing.md,
                    vertical = spacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(state.results, key = { it.id }) { song ->
                    KaraokeSongRow(
                        song = song,
                        showSyncedHint = false,
                        onClick = { onSongSelected(song.id) },
                    )
                }
            }
        }
        state.isLoadingSuggestions -> {
            LoadingCentered()
        }
        state.suggestions.isEmpty() -> {
            EmptyState(
                title = stringResource(R.string.karaoke_empty_title),
                description = stringResource(R.string.karaoke_empty_description),
                icon = Icons.Default.Mic,
                modifier = Modifier.padding(top = spacing.xl),
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = spacing.md,
                    vertical = spacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                item {
                    Column(modifier = Modifier.padding(vertical = spacing.sm)) {
                        Text(
                            text = stringResource(R.string.karaoke_suggestions_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.karaoke_suggestions_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.suggestions, key = { it.id }) { song ->
                    KaraokeSongRow(
                        song = song,
                        showSyncedHint = true,
                        onClick = { onSongSelected(song.id) },
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
private fun KaraokeSongRow(
    song: Song,
    showSyncedHint: Boolean,
    onClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        MeloImage(
            imageUrl = song.coverUrl.ifBlank { null },
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
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
            if (showSyncedHint) {
                Text(
                    text = stringResource(R.string.karaoke_synced_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Icon(
            Icons.Default.Mic,
            contentDescription = null,
            tint = scheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
    }
}
