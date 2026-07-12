package com.melonet.app.feature.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    listType: LibraryListType,
    viewModel: LibrarySongsViewModel,
    onNavigateToPlayer: (String) -> Unit,
    onPlayQueue: (startSongId: String, songs: List<Song>, shuffle: Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val songs = viewModel.songs.collectAsLazyPagingItems()
    val spacing = MeloNetTheme.spacing

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
                onClick = { viewModel.handleEvent(LibrarySongsContract.Event.PlayAll) },
                modifier = Modifier.weight(1f),
            )
            MeloButton(
                text = stringResource(R.string.library_shuffle),
                onClick = { viewModel.handleEvent(LibrarySongsContract.Event.ShuffleAll) },
                modifier = Modifier.weight(1f),
                variant = MeloButtonVariant.Outlined,
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
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
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_song),
                                tint = MaterialTheme.colorScheme.error,
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
