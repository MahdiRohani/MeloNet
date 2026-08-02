package com.melonet.app.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.ShimmerBox
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    listType: LibraryListType,
    viewModel: LibrarySongsViewModel,
    onNavigateToPlayer: (String) -> Unit,
    onPlayQueue: (startSongId: String, songs: List<Song>, shuffle: Boolean) -> Unit,
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val songs = viewModel.songs.collectAsLazyPagingItems()
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(listType) {
        viewModel.setListType(listType)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LibrarySongsContract.Effect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is LibrarySongsContract.Effect.PlayQueue -> {
                    onPlayQueue(effect.startSongId, viewModel.getCachedSongs(), effect.shuffle)
                }
            }
        }
    }

    LaunchedEffect(songs.itemSnapshotList.items) {
        viewModel.updateCachedSongs(songs.itemSnapshotList.items)
    }

    val title = when (state.listType) {
        LibraryListType.LIKED -> stringResource(R.string.profile_liked_songs)
        LibraryListType.RECENT -> stringResource(R.string.home_quick_action_recent)
    }
    val emptyDescription = when (state.listType) {
        LibraryListType.LIKED -> stringResource(R.string.library_empty_liked_description)
        LibraryListType.RECENT -> stringResource(R.string.library_empty_recent_description)
    }
    val emptyIcon = when (state.listType) {
        LibraryListType.LIKED -> Icons.Outlined.FavoriteBorder
        LibraryListType.RECENT -> Icons.Outlined.History
    }
    val coverUrls = songs.itemSnapshotList.items.take(4).map { it.coverUrl }
    val songCount = songs.itemCount
    val isRefreshing = songs.loadState.refresh is LoadState.Loading && songs.itemCount == 0
    val isEmpty = songs.itemCount == 0 && songs.loadState.refresh is LoadState.NotLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            item(key = "hero") {
                LibraryHeroHeader(
                    title = title,
                    songCount = songCount,
                    coverUrls = coverUrls,
                    canPlay = songCount > 0,
                    onPlayAll = { viewModel.handleEvent(LibrarySongsContract.Event.PlayAll) },
                    onShuffle = { viewModel.handleEvent(LibrarySongsContract.Event.ShuffleAll) },
                    onBack = onNavigateBack,
                )
            }

            when {
                isRefreshing -> {
                    items(6, key = { "shim_$it" }) {
                        SongRowShimmer(modifier = Modifier.padding(horizontal = spacing.md))
                    }
                }
                isEmpty -> {
                    item(key = "library_empty") {
                        EmptyState(
                            title = stringResource(R.string.library_empty_songs),
                            description = emptyDescription,
                            icon = emptyIcon,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                        )
                    }
                }
                else -> {
                    items(
                        count = songs.itemCount,
                        key = songs.itemKey { it.id },
                    ) { index ->
                        val song = songs[index] ?: return@items
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.handleEvent(LibrarySongsContract.Event.DismissSong(song.id))
                                    true
                                } else {
                                    false
                                }
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_delete_song),
                                        tint = scheme.error,
                                        modifier = Modifier.padding(spacing.md),
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                        ) {
                            SongListItem(
                                song = song,
                                onClick = {
                                    viewModel.handleEvent(LibrarySongsContract.Event.SongClicked(song.id))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryHeroHeader(
    title: String,
    songCount: Int,
    coverUrls: List<String>,
    canPlay: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
    ) {
        CoverMosaic(
            coverUrls = coverUrls,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            scheme.background.copy(alpha = 0.55f),
                            scheme.background,
                        ),
                    ),
                ),
        )
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                        tint = Color.White,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .padding(bottom = spacing.lg),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (songCount > 0) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = pluralStringResource(R.plurals.artist_song_count, songCount, songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(spacing.md))
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                MeloButton(
                    text = stringResource(R.string.library_play_all),
                    onClick = onPlayAll,
                    enabled = canPlay,
                    modifier = Modifier.weight(1f),
                )
                MeloButton(
                    text = stringResource(R.string.library_shuffle),
                    onClick = onShuffle,
                    enabled = canPlay,
                    variant = MeloButtonVariant.Outlined,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CoverMosaic(
    coverUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val urls = coverUrls.map { it.trim() }.filter { it.isNotEmpty() }
    Box(
        modifier = modifier.background(scheme.primary.copy(alpha = 0.22f)),
    ) {
        when {
            urls.isEmpty() -> Unit
            urls.size == 1 -> {
                MeloImage(
                    imageUrl = urls.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    targetSize = 420.dp,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                val tiles = urls.take(4)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        tiles.getOrNull(0)?.let { url ->
                            MeloImage(
                                imageUrl = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                            )
                        }
                        tiles.getOrNull(1)?.let { url ->
                            MeloImage(
                                imageUrl = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                            )
                        }
                    }
                    if (tiles.size > 2) {
                        Row(modifier = Modifier.weight(1f)) {
                            tiles.getOrNull(2)?.let { url ->
                                MeloImage(
                                    imageUrl = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                )
                            }
                            tiles.getOrNull(3)?.let { url ->
                                MeloImage(
                                    imageUrl = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                )
                            } ?: Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongRowShimmer(modifier: Modifier = Modifier) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        ShimmerBox(
            modifier = Modifier
                .size(dimensions.iconLg)
                .aspectRatio(1f),
            shape = MaterialTheme.shapes.small,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(14.dp),
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp),
            )
        }
    }
}
