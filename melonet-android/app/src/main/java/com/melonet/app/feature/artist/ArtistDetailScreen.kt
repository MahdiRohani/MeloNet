package com.melonet.app.feature.artist

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.common.displayMessage
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.SortFilterRow
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song
import com.melonet.app.feature.playlists.SongListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: Int,
    viewModel: ArtistDetailViewModel,
    onNavigateBack: () -> Unit,
    onPlayQueue: (startSongId: String, songs: List<Song>) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val songs = viewModel.songs.collectAsLazyPagingItems()

    LaunchedEffect(artistId) {
        viewModel.handleEvent(ArtistDetailContract.Event.Load(artistId))
    }

    LaunchedEffect(songs.itemSnapshotList.items) {
        viewModel.updateCachedSongs(songs.itemSnapshotList.items)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ArtistDetailContract.Effect.PlayQueue ->
                    onPlayQueue(effect.startSongId, viewModel.getCachedSongs())
                is ArtistDetailContract.Effect.ShowError -> Unit
            }
        }
    }

    val firstSongId = songs.itemSnapshotList.items.firstOrNull()?.id

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
                ArtistHeroHeader(
                    name = state.artist?.name.orEmpty(),
                    imageUrl = state.artist?.imageUrl,
                    songCount = state.artist?.songCount ?: 0,
                    isFollowing = state.artist?.isFollowing == true,
                    isFollowLoading = state.isFollowLoading,
                    canPlay = firstSongId != null,
                    onToggleFollow = { viewModel.handleEvent(ArtistDetailContract.Event.ToggleFollow) },
                    onPlayAll = {
                        firstSongId?.let { id ->
                            viewModel.handleEvent(ArtistDetailContract.Event.SongClicked(id))
                        }
                    },
                    onBack = onNavigateBack,
                )
            }

            item(key = "sort") {
                SortFilterRow(
                    selected = state.sort,
                    onSelected = { viewModel.handleEvent(ArtistDetailContract.Event.SortSelected(it)) },
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                )
            }

            when {
                songs.loadState.refresh is LoadState.Loading && songs.itemCount == 0 -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.xl),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = scheme.primary)
                        }
                    }
                }
                songs.loadState.refresh is LoadState.Error && songs.itemCount == 0 -> {
                    item {
                        ErrorState(
                            message = state.error?.displayMessage(context)
                                ?: stringResource(R.string.catalog_error),
                            onRetry = { songs.retry() },
                        )
                    }
                }
                songs.itemCount == 0 && songs.loadState.refresh is LoadState.NotLoading -> {
                    item { EmptyState(title = stringResource(R.string.catalog_empty)) }
                }
                else -> {
                    items(
                        count = songs.itemCount,
                        key = songs.itemKey { it.id },
                    ) { index ->
                        val song = songs[index] ?: return@items
                        SongListItem(
                            song = song,
                            onClick = {
                                viewModel.handleEvent(ArtistDetailContract.Event.SongClicked(song.id))
                            },
                        )
                    }
                    if (songs.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = scheme.primary)
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
private fun ArtistHeroHeader(
    name: String,
    imageUrl: String?,
    songCount: Int,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    canPlay: Boolean,
    onToggleFollow: () -> Unit,
    onPlayAll: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
    ) {
        MeloImage(
            imageUrl = imageUrl?.ifBlank { null },
            contentDescription = name,
            contentScale = ContentScale.Crop,
            targetSize = 420.dp,
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
                text = name.ifBlank { stringResource(R.string.artist_title) },
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                MeloButton(
                    text = stringResource(R.string.library_play_all),
                    onClick = onPlayAll,
                    enabled = canPlay,
                    variant = MeloButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                )
                MeloButton(
                    text = stringResource(
                        if (isFollowing) R.string.social_unfollow else R.string.social_follow,
                    ),
                    onClick = onToggleFollow,
                    enabled = !isFollowLoading,
                    variant = if (isFollowing) MeloButtonVariant.Outlined else MeloButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
