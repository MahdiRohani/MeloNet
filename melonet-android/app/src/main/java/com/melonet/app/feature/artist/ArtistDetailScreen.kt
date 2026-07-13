package com.melonet.app.feature.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text(state.artist?.name ?: stringResource(R.string.artist_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            item(key = "header") {
                ArtistHeader(
                    name = state.artist?.name.orEmpty(),
                    imageUrl = state.artist?.imageUrl,
                    songCount = state.artist?.songCount ?: 0,
                    isFollowing = state.artist?.isFollowing == true,
                    isFollowLoading = state.isFollowLoading,
                    onToggleFollow = { viewModel.handleEvent(ArtistDetailContract.Event.ToggleFollow) },
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
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    name: String,
    imageUrl: String?,
    songCount: Int,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    onToggleFollow: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        MeloImage(
            imageUrl = imageUrl?.ifBlank { null },
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (songCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.artist_song_count, songCount, songCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MeloButton(
            text = stringResource(
                if (isFollowing) R.string.social_unfollow else R.string.social_follow,
            ),
            onClick = onToggleFollow,
            enabled = !isFollowLoading,
            variant = if (isFollowing) MeloButtonVariant.Outlined else MeloButtonVariant.Primary,
        )
    }
}
