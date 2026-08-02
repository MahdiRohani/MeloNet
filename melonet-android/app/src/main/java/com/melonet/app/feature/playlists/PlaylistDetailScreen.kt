package com.melonet.app.feature.playlists

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    viewModel: PlaylistDetailViewModel,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToAddSongs: (Int) -> Unit,
    onPlayQueue: (startSongId: String, songs: List<Song>, shuffle: Boolean) -> Unit,
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val songs = viewModel.songs.collectAsLazyPagingItems()
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val playlist = state.playlist
    val isOwner = playlist?.isOwner == true
    val isRefreshing = songs.loadState.refresh is LoadState.Loading && songs.itemCount == 0
    val isEmpty = songs.itemCount == 0 && songs.loadState.refresh is LoadState.NotLoading

    LaunchedEffect(playlistId) {
        viewModel.handleEvent(PlaylistDetailContract.Event.Load(playlistId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlaylistDetailContract.Effect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is PlaylistDetailContract.Effect.PlayQueue -> {
                    val queue = viewModel.getCachedSongs()
                    if (queue.isNotEmpty()) {
                        onPlayQueue(effect.startSongId, queue, effect.shuffle)
                    }
                }
                is PlaylistDetailContract.Effect.ShowError -> Unit
            }
        }
    }

    LaunchedEffect(songs.itemSnapshotList.items) {
        viewModel.updateCachedSongs(songs.itemSnapshotList.items.filterNotNull())
    }

    // Ensure list reloads when returning to this screen (e.g. after adding songs).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, playlistId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSongs()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        when {
            state.isLoading && playlist == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    item(key = "hero") {
                        PlaylistHeroHeader(
                            title = playlist?.title.orEmpty(),
                            coverUrls = playlist?.displayCoverUrls.orEmpty(),
                            songCount = playlist?.songCount ?: songs.itemCount,
                            canPlay = songs.itemCount > 0,
                            showAdd = isOwner,
                            onPlayAll = { viewModel.handleEvent(PlaylistDetailContract.Event.PlayAll) },
                            onShuffle = { viewModel.handleEvent(PlaylistDetailContract.Event.ShuffleAll) },
                            onAdd = { onNavigateToAddSongs(playlistId) },
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
                            item(key = "empty") {
                                EmptyState(
                                    title = stringResource(R.string.playlist_detail_empty),
                                    description = stringResource(R.string.playlist_detail_empty_description),
                                    icon = Icons.Outlined.LibraryMusic,
                                    actionLabel = if (isOwner) {
                                        stringResource(R.string.playlist_detail_add_songs)
                                    } else {
                                        null
                                    },
                                    onAction = if (isOwner) {
                                        { onNavigateToAddSongs(playlistId) }
                                    } else {
                                        null
                                    },
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
                                SongListItem(
                                    song = song,
                                    index = index + 1,
                                    onClick = {
                                        viewModel.handleEvent(PlaylistDetailContract.Event.SongClicked(song))
                                    },
                                    onMoreClick = if (isOwner) {
                                        {
                                            viewModel.handleEvent(
                                                PlaylistDetailContract.Event.RemoveSong(song.id),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    moreLabel = stringResource(R.string.playlist_remove_song),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistHeroHeader(
    title: String,
    coverUrls: List<String>,
    songCount: Int,
    canPlay: Boolean,
    showAdd: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
    ) {
        com.melonet.app.core.designsystem.component.CoverMosaic(
            coverUrls = coverUrls,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            scheme.background.copy(alpha = 0.6f),
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
                text = title.ifBlank { stringResource(R.string.playlists_library_title) },
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
            if (showAdd) {
                Spacer(modifier = Modifier.height(spacing.sm))
                MeloButton(
                    text = stringResource(R.string.playlist_detail_add_songs),
                    onClick = onAdd,
                    variant = MeloButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int? = null,
    onMoreClick: (() -> Unit)? = null,
    moreLabel: String? = null,
) {
    val dimensions = MeloNetTheme.dimensions
    var menuExpanded by remember { mutableStateOf(false) }

    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = song.artistName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index != null) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(28.dp),
                    )
                }
                MeloImage(
                    imageUrl = song.coverUrl.ifBlank { null },
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(dimensions.iconLg)
                        .clip(MaterialTheme.shapes.small),
                )
            }
        },
        trailingContent = if (onMoreClick != null) {
            {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_more),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(moreLabel ?: stringResource(R.string.playlist_remove_song))
                            },
                            onClick = {
                                menuExpanded = false
                                onMoreClick()
                            },
                        )
                    }
                }
            }
        } else {
            null
        },
    )
}
