package com.melonet.app.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.QuickActionChip
import com.melonet.app.core.designsystem.component.QuickActionChipShimmer
import com.melonet.app.core.designsystem.component.SectionHeader
import com.melonet.app.core.designsystem.component.SectionHeaderShimmer
import com.melonet.app.core.designsystem.component.SongCard
import com.melonet.app.core.designsystem.component.SongCardShimmer
import com.melonet.app.core.designsystem.component.shimmerEffect
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.QuickAction
import com.melonet.app.data.model.Song

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongClick: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeContract.Effect.NavigateToPlayer -> onSongClick(effect.songId)
                is HomeContract.Effect.ShowError -> Unit
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            HomeSlider(
                isLoading = state.isLoading,
                carouselTitle = state.feed?.carousel?.firstOrNull()?.title
            )
        }

        item {
            QuickActionsSection(
                actions = state.feed?.quickActions.orEmpty(),
                isLoading = state.isLoading,
                onActionClick = { viewModel.handleEvent(HomeContract.Event.QuickActionClicked(it)) }
            )
        }

        if (state.isLoading) {
            items(3) {
                SongSection(
                    title = null,
                    songs = emptyList(),
                    isLoading = true,
                    onSongClick = {}
                )
            }
        } else {
            items(state.feed?.rows.orEmpty(), key = { it.id }) { row ->
                SongSection(
                    title = row.title,
                    songs = row.items,
                    isLoading = false,
                    onSongClick = { songId ->
                        viewModel.handleEvent(HomeContract.Event.SongClicked(songId))
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(spacing.lg)) }
    }
}

@Composable
private fun HomeSlider(isLoading: Boolean, carouselTitle: String?) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions.carouselHeight)
            .padding(spacing.md)
            .clip(MaterialTheme.shapes.large)
            .then(
                if (isLoading) Modifier.shimmerEffect()
                else Modifier.background(MaterialTheme.colorScheme.primary)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoading) {
            Text(
                text = carouselTitle ?: stringResource(R.string.home_carousel_fallback),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    actions: List<QuickAction>,
    isLoading: Boolean,
    onActionClick: (QuickAction) -> Unit
) {
    val spacing = MeloNetTheme.spacing

    val defaultActions = listOf(
        Triple(stringResource(R.string.home_quick_action_liked), Icons.Default.Favorite, null),
        Triple(stringResource(R.string.home_quick_action_recent), Icons.Default.History, null),
        Triple(stringResource(R.string.home_quick_action_playlists), Icons.Default.LibraryMusic, null),
        Triple(stringResource(R.string.home_quick_action_following), Icons.Default.People, null)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isLoading) {
            repeat(4) {
                QuickActionChipShimmer()
            }
        } else if (actions.isNotEmpty()) {
            actions.take(4).forEach { action ->
                QuickActionChip(
                    title = action.title,
                    icon = iconForQuickAction(action),
                    onClick = { onActionClick(action) }
                )
            }
        } else {
            defaultActions.forEach { (title, icon, _) ->
                QuickActionChip(title = title, icon = icon, onClick = {})
            }
        }
    }
}

private fun iconForQuickAction(action: QuickAction): ImageVector {
    val key = (action.icon ?: action.target ?: action.id).lowercase()
    return when {
        "liked" in key || "favorite" in key -> Icons.Default.Favorite
        "recent" in key || "history" in key -> Icons.Default.History
        "playlist" in key -> Icons.Default.LibraryMusic
        "follow" in key || "artist" in key || "people" in key -> Icons.Default.People
        else -> Icons.Default.Favorite
    }
}

@Composable
private fun SongSection(
    title: String?,
    songs: List<Song>,
    isLoading: Boolean,
    onSongClick: (Int) -> Unit
) {
    val spacing = MeloNetTheme.spacing

    Column(modifier = Modifier.padding(vertical = spacing.sm + spacing.xs)) {
        when {
            isLoading -> SectionHeaderShimmer()
            !title.isNullOrBlank() -> SectionHeader(title = title)
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            if (isLoading) {
                items(5) { SongCardShimmer() }
            } else {
                items(songs, key = { it.id }) { song ->
                    SongCard(
                        title = song.title,
                        subtitle = song.artistName,
                        imageUrl = song.coverUrl,
                        onClick = { onSongClick(song.id) }
                    )
                }
            }
        }
    }
}
