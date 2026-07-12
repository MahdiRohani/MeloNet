package com.melonet.app.feature.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song

@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    viewModel: PlaylistDetailViewModel,
    onNavigateToPlayer: (Int) -> Unit,
    onPlayQueue: (startSongId: Int, songs: List<Song>, shuffle: Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val songs = viewModel.songs.collectAsLazyPagingItems()
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions

    LaunchedEffect(playlistId) {
        viewModel.handleEvent(PlaylistDetailContract.Event.Load(playlistId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlaylistDetailContract.Effect.NavigateToPlayer -> onNavigateToPlayer(effect.songId)
                is PlaylistDetailContract.Effect.PlayQueue -> {
                    onPlayQueue(effect.startSongId, viewModel.getCachedSongs(), effect.shuffle)
                }
                is PlaylistDetailContract.Effect.ShowError -> { }
            }
        }
    }

    LaunchedEffect(songs.itemSnapshotList.items) {
        viewModel.updateCachedSongs(songs.itemSnapshotList.items)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading && state.playlist == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }

        state.playlist?.let { playlist ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MeloImage(
                    imageUrl = playlist.coverUrl.ifBlank { null },
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(dimensions.avatarLg)
                        .clip(MaterialTheme.shapes.large),
                )
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.playlists_song_count, playlist.songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    MeloButton(
                        text = stringResource(R.string.library_play_all),
                        onClick = { viewModel.handleEvent(PlaylistDetailContract.Event.PlayAll) },
                        modifier = Modifier.weight(1f),
                    )
                    MeloButton(
                        text = stringResource(R.string.library_shuffle),
                        onClick = { viewModel.handleEvent(PlaylistDetailContract.Event.ShuffleAll) },
                        modifier = Modifier.weight(1f),
                        variant = com.melonet.app.core.designsystem.component.MeloButtonVariant.Outlined,
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            items(
                count = songs.itemCount,
                key = songs.itemKey { it.id },
            ) { index ->
                val song = songs[index] ?: return@items
                SongListItem(
                    song = song,
                    onClick = {
                        viewModel.handleEvent(PlaylistDetailContract.Event.SongClicked(song.id))
                    },
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
) {
    val dimensions = MeloNetTheme.dimensions
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
            MeloImage(
                imageUrl = song.coverUrl.ifBlank { null },
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(dimensions.iconLg)
                    .clip(MaterialTheme.shapes.small),
            )
        },
    )
}
