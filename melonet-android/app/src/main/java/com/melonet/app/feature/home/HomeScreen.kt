package com.melonet.app.feature.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onOpenKaraoke: () -> Unit = {},
    onOpenVoiceCover: () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val errorMessage = state.error?.displayMessage(context)
    val listState = rememberLazyListState()
    val showSkeleton = state.isLoading && state.feed == null

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
        errorMessage != null && state.feed == null && !state.isLoading -> {
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
                    isLoading = showSkeleton,
                    listState = listState,
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
                    onKaraokeClick = onOpenKaraoke,
                    onVoiceCoverClick = onOpenVoiceCover,
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
    listState: LazyListState,
    onSongClick: (Song) -> Unit,
    onQuickActionClick: (QuickAction) -> Unit,
    onSeeAllClick: (HomeRow) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onKaraokeClick: () -> Unit,
    onVoiceCoverClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing

    val carouselCategories = remember(rows) {
        rows
            .filter { it.items.isNotEmpty() }
            .map { row ->
                CarouselCategory(
                    title = row.title,
                    coverUrl = row.items.first().coverUrl,
                    row = row,
                )
            }
    }
    val visibleArtistRows = remember(artistRows) {
        artistRows.filter { it.items.isNotEmpty() }
    }
    val feedItems = remember(rows) {
        buildList {
            rows.forEach { row ->
                add(HomeFeedItem.SongRow(row))
                if (row.id == "new") {
                    add(HomeFeedItem.VoiceCoverBanner)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "carousel", contentType = "carousel") {
            HomeCarousel(
                categories = carouselCategories,
                isLoading = isLoading,
                listState = listState,
                onCategoryClick = onSeeAllClick,
            )
        }

        item(key = "quick_actions", contentType = "quick_actions") {
            QuickActionsSection(
                actions = quickActions,
                isLoading = isLoading,
                onActionClick = onQuickActionClick,
            )
        }

        if (!isLoading) {
            item(key = "karaoke_banner", contentType = "banner") {
                KaraokeBanner(onClick = onKaraokeClick)
            }
        }

        if (isLoading) {
            items(3, key = { "song_skeleton_$it" }, contentType = { "song_skeleton" }) {
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
            items(
                items = feedItems,
                key = { item ->
                    when (item) {
                        is HomeFeedItem.SongRow -> item.row.id
                        HomeFeedItem.VoiceCoverBanner -> "voice_cover_banner"
                    }
                },
                contentType = { item ->
                    when (item) {
                        is HomeFeedItem.SongRow -> "song_row"
                        HomeFeedItem.VoiceCoverBanner -> "banner"
                    }
                },
            ) { item ->
                when (item) {
                    is HomeFeedItem.SongRow -> {
                        SongSection(
                            title = item.row.title,
                            songs = item.row.items,
                            seeAllPath = item.row.seeAllPath,
                            isLoading = false,
                            onSongClick = onSongClick,
                            onSeeAllClick = { onSeeAllClick(item.row) },
                        )
                    }
                    HomeFeedItem.VoiceCoverBanner -> {
                        VoiceCoverBanner(onClick = onVoiceCoverClick)
                    }
                }
            }

            items(
                items = visibleArtistRows,
                key = { it.id },
                contentType = { "artist_row" },
            ) { row ->
                ArtistSection(
                    title = row.title,
                    artists = row.items,
                    onArtistClick = onArtistClick,
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(spacing.lg))
        }
    }
}

private sealed interface HomeFeedItem {
    data class SongRow(val row: HomeRow) : HomeFeedItem
    data object VoiceCoverBanner : HomeFeedItem
}

@Composable
private fun KaraokeBanner(onClick: () -> Unit) {
    HomeFeatureBanner(
        onClick = onClick,
        icon = Icons.Default.Mic,
        title = stringResource(R.string.karaoke_banner_title),
        subtitle = stringResource(R.string.karaoke_banner_subtitle),
        gradient = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MeloNetTheme.colors.premium,
        ),
    )
}

@Composable
private fun VoiceCoverBanner(onClick: () -> Unit) {
    HomeFeatureBanner(
        onClick = onClick,
        icon = Icons.Default.RecordVoiceOver,
        title = stringResource(R.string.voice_cover_banner_title),
        subtitle = stringResource(R.string.voice_cover_banner_subtitle),
        gradient = listOf(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary,
            MeloNetTheme.colors.premium,
        ),
    )
}

@Composable
private fun HomeFeatureBanner(
    onClick: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<androidx.compose.ui.graphics.Color>,
) {
    val spacing = MeloNetTheme.spacing
    val onBanner = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(onBanner.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onBanner,
                )
            }
            Spacer(modifier = Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onBanner,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onBanner.copy(alpha = 0.85f),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = onBanner,
            )
        }
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
                    imageCrossfade = false,
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
                items(5, key = { "shimmer_$it" }) {
                    SongCardShimmer()
                }
            } else {
                items(songs, key = { it.id }) { song ->
                    SongCard(
                        title = song.title,
                        subtitle = song.artistName,
                        imageUrl = song.coverUrl,
                        // No shared elements / press springs in dense Home rows.
                        enablePressAnimation = false,
                        imageCrossfade = false,
                        onClick = { onSongClick(song) },
                    )
                }
            }
        }
    }
}
