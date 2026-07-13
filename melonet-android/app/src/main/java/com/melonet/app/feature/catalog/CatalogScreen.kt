package com.melonet.app.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song
import com.melonet.app.feature.playlists.SongListItem

@Composable
fun CatalogScreen(
    listType: String,
    filter: String?,
    viewModel: CatalogViewModel,
    onPlayQueue: (startSongId: String, songs: List<Song>, shuffle: Boolean) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val songs = viewModel.songs.collectAsLazyPagingItems()

    LaunchedEffect(listType, filter) {
        viewModel.configure(listType, filter)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CatalogContract.Effect.PlayQueue ->
                    onPlayQueue(effect.startSongId, viewModel.getCachedSongs(), effect.shuffle)
            }
        }
    }

    LaunchedEffect(songs.itemSnapshotList.items) {
        viewModel.updateCachedSongs(songs.itemSnapshotList.items)
    }

    val title = catalogTitle(listType, filter)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.md),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = spacing.md),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            MeloButton(
                text = stringResource(R.string.library_play_all),
                onClick = { viewModel.handleEvent(CatalogContract.Event.PlayAll) },
                modifier = Modifier.weight(1f),
            )
            MeloButton(
                text = stringResource(R.string.library_shuffle),
                onClick = { viewModel.handleEvent(CatalogContract.Event.ShuffleAll) },
                modifier = Modifier.weight(1f),
                variant = MeloButtonVariant.Outlined,
            )
        }

        when {
            songs.loadState.refresh is LoadState.Loading && songs.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            songs.loadState.refresh is LoadState.Error && songs.itemCount == 0 -> {
                val error = (songs.loadState.refresh as LoadState.Error).error.message
                    ?: stringResource(R.string.catalog_error)
                ErrorState(message = error, onRetry = { songs.retry() })
            }
            songs.itemCount == 0 && songs.loadState.refresh is LoadState.NotLoading -> {
                EmptyState(title = stringResource(R.string.catalog_empty))
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = spacing.md),
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
                                viewModel.handleEvent(CatalogContract.Event.SongClicked(song.id))
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
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(spacing.xs),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun catalogTitle(listType: String, filter: String?): String = when (listType.lowercase()) {
    "popular" -> stringResource(R.string.home_quick_action_popular)
    "new" -> stringResource(R.string.home_quick_action_new)
    "trending" -> stringResource(R.string.catalog_trending)
    "category" -> when (filter?.lowercase()) {
        "global" -> stringResource(R.string.home_quick_action_global)
        "iranian" -> stringResource(R.string.catalog_iranian)
        "nostalgia" -> stringResource(R.string.catalog_nostalgia)
        "popular" -> stringResource(R.string.home_quick_action_popular)
        else -> filter ?: stringResource(R.string.nav_home)
    }
    else -> filter ?: listType
}
