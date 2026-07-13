package com.melonet.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.common.displayMessage
import com.melonet.app.core.designsystem.component.ArtistCircleItem
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.QuickActionChip
import com.melonet.app.core.designsystem.component.QuickActionChipShimmer
import com.melonet.app.core.designsystem.component.SectionHeader
import com.melonet.app.core.designsystem.component.SectionHeaderShimmer
import com.melonet.app.core.designsystem.component.SongCard
import com.melonet.app.core.designsystem.component.SongCardShimmer
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.ui.PlayerSharedKeys
import com.melonet.app.data.model.Artist
import com.melonet.app.data.model.HomeArtistRow
import com.melonet.app.data.model.HomeRow
import com.melonet.app.data.model.QuickAction
import com.melonet.app.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlaySong: (Song, List<Song>) -> Unit,
    onNavigate: (Any) -> Unit,
    snackbarHostState: SnackbarHostState? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val errorMessage = state.error?.displayMessage(context)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeContract.Effect.PlaySong -> onPlaySong(effect.song, effect.queue)
                is HomeContract.Effect.Navigate -> onNavigate(effect.destination.toRoute())
                is HomeContract.Effect.ShowError -> {
                    snackbarHostState?.showSnackbar(effect.error.displayMessage(context))
                }
            }
        }
    }

    when {
        state.isLoading -> {
            HomeFeedContent(
                quickActions = emptyList(),
                rows = emptyList(),
                artistRows = emptyList(),
                isLoading = true,
                onSongClick = {},
                onQuickActionClick = {},
                onSeeAllClick = {},
                onArtistClick = {},
            )
        }
        errorMessage != null && state.feed == null -> {
            ErrorState(
                message = errorMessage,
                onRetry = { viewModel.handleEvent(HomeContract.Event.Load) },
            )
        }
        state.feed != null && state.feed!!.isEmpty -> {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                description = stringResource(R.string.home_empty_description),
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.handleEvent(HomeContract.Event.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                HomeFeedContent(
                    quickActions = state.feed?.quickActions.orEmpty(),
                    rows = state.feed?.rows.orEmpty(),
                    artistRows = state.feed?.artistRows.orEmpty(),
                    isLoading = false,
                    onSongClick = { song ->
                        viewModel.handleEvent(HomeContract.Event.SongClicked(song))
                    },
                    onQuickActionClick = { action ->
                        viewModel.handleEvent(HomeContract.Event.QuickActionClicked(action))
                    },
                    onSeeAllClick = { row ->
                        viewModel.handleEvent(HomeContract.Event.SeeAllClicked(row))
                    },
                    onArtistClick = { artist ->
                        viewModel.handleEvent(HomeContract.Event.ArtistClicked(artist))
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeFeedContent(
    quickActions: List<QuickAction>,
    rows: List<HomeRow>,
    artistRows: List<HomeArtistRow>,
    isLoading: Boolean,
    onSongClick: (Song) -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    onSeeAllClick: (HomeRow) -> Unit,
    onArtistClick: (Artist) -> Unit,
) {
    val spacing = MeloNetTheme.spacing

    val carouselCategories = rows
        .filter { it.items.isNotEmpty() }
        .map { row ->
            CarouselCategory(
                title = row.title,
                coverUrl = row.items.first().coverUrl,
                row = row,
            )
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        item {
            HomeCarousel(
                categories = carouselCategories,
                isLoading = isLoading,
                onCategoryClick = onSeeAllClick,
            )
        }

        item {
            QuickActionsSection(
                actions = quickActions,
                isLoading = isLoading,
                onActionClick = onQuickActionClick,
            )
        }

        if (isLoading) {
            items(3) {
                SongSection(
                    title = null,
                    songs = emptyList(),
                    seeAllPath = null,
                    isLoading = true,
                    onSongClick = {},
                    onSeeAllClick = {},
                )
            }
        } else {
            items(rows, key = { it.id }) { row ->
                SongSection(
                    title = row.title,
                    songs = row.items,
                    seeAllPath = row.seeAllPath,
                    isLoading = false,
                    onSongClick = onSongClick,
                    onSeeAllClick = { onSeeAllClick(row) },
                )
            }

            items(artistRows.filter { it.items.isNotEmpty() }, key = { it.id }) { row ->
                ArtistSection(
                    title = row.title,
                    artists = row.items,
                    onArtistClick = onArtistClick,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(spacing.lg)) }
    }
}

@Composable
private fun ArtistSection(
    title: String,
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    Column(modifier = Modifier.padding(vertical = spacing.sm + spacing.xs)) {
        SectionHeader(title = title, onActionClick = null)
        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            items(artists, key = { it.id }) { artist ->
                ArtistCircleItem(
                    name = artist.name,
                    imageUrl = artist.imageUrl,
                    onClick = { onArtistClick(artist) },
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    actions: List<QuickAction>,
    isLoading: Boolean,
    onActionClick: (QuickAction) -> Unit,
) {
    val spacing = MeloNetTheme.spacing

    val fallbackActions = remember {
        listOf(
            QuickAction("liked", "", "liked", "favorite"),
            QuickAction("recent", "", "recent", "history"),
            QuickAction("playlists", "", "playlists", "playlist"),
            QuickAction("following", "", "following", "people"),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (isLoading) {
            repeat(4) { QuickActionChipShimmer() }
        } else {
            val displayActions = if (actions.isNotEmpty()) actions.take(4) else fallbackActions
            displayActions.forEach { action ->
                QuickActionChip(
                    title = action.title.ifBlank {
                        fallbackLabelForAction(action)
                    },
                    icon = iconForQuickAction(action),
                    onClick = { onActionClick(action) },
                )
            }
        }
    }
}

@Composable
private fun fallbackLabelForAction(action: QuickAction): String {
    val key = (action.icon ?: action.target ?: action.id).lowercase()
    return when {
        "liked" in key || "favorite" in key -> stringResource(R.string.home_quick_action_liked)
        "recent" in key || "history" in key -> stringResource(R.string.home_quick_action_recent)
        "playlist" in key -> stringResource(R.string.home_quick_action_playlists)
        "follow" in key || "people" in key -> stringResource(R.string.home_quick_action_following)
        "search" in key -> stringResource(R.string.nav_search)
        "popular" in key || "trending" in key -> stringResource(R.string.home_quick_action_popular)
        "new" in key -> stringResource(R.string.home_quick_action_new)
        "iranian" in key || "persian" in key -> stringResource(R.string.home_quick_action_iranian)
        "turkish" in key -> stringResource(R.string.home_quick_action_turkish)
        "instrumental" in key -> stringResource(R.string.home_quick_action_instrumental)
        "global" in key || "public" in key -> stringResource(R.string.home_quick_action_global)
        else -> action.id
    }
}

private fun iconForQuickAction(action: QuickAction): ImageVector {
    val key = (action.icon ?: action.target ?: action.id).lowercase()
    return when {
        "search" in key -> Icons.Default.Search
        "popular" in key || "trending" in key -> Icons.Default.TrendingUp
        "new" in key -> Icons.Default.NewReleases
        "iranian" in key || "persian" in key -> Icons.Default.Flag
        "turkish" in key -> Icons.Default.Public
        "instrumental" in key -> Icons.Default.MusicNote
        "global" in key || "public" in key -> Icons.Default.Public
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
    seeAllPath: String?,
    isLoading: Boolean,
    onSongClick: (Song) -> Unit,
    onSeeAllClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing

    Column(modifier = Modifier.padding(vertical = spacing.sm + spacing.xs)) {
        when {
            isLoading -> SectionHeaderShimmer()
            !title.isNullOrBlank() -> SectionHeader(
                title = title,
                onActionClick = if (!seeAllPath.isNullOrBlank()) onSeeAllClick else null,
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (isLoading) {
                items(5) { SongCardShimmer() }
            } else {
                items(songs, key = { it.id }) { song ->
                    SongCard(
                        title = song.title,
                        subtitle = song.artistName,
                        imageUrl = song.coverUrl,
                        sharedTransitionKey = PlayerSharedKeys.songCover(song.id),
                        onClick = { onSongClick(song) },
                    )
                }
            }
        }
    }
}
