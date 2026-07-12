package com.melonet.app.feature.playlists

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.PlaylistCard
import com.melonet.app.core.designsystem.component.QuickActionChip
import com.melonet.app.core.designsystem.component.SectionHeader
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToLiked: () -> Unit,
    onNavigateToRecent: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val systemPlaylists = viewModel.systemPlaylistsFlow.collectAsLazyPagingItems()
    val userPlaylists = viewModel.userPlaylistsFlow.collectAsLazyPagingItems()
    val spacing = MeloNetTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlaylistsContract.Effect.NavigateToDetail -> onNavigateToDetail(effect.playlistId)
                PlaylistsContract.Effect.NavigateToLiked -> onNavigateToLiked()
                PlaylistsContract.Effect.NavigateToRecent -> onNavigateToRecent()
                is PlaylistsContract.Effect.ShowError -> { /* snackbar in shell */ }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.handleEvent(PlaylistsContract.Event.ShowCreateDialog) },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_create_playlist),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    QuickActionChip(
                        title = stringResource(R.string.profile_liked_songs),
                        icon = Icons.Default.Favorite,
                        onClick = { viewModel.handleEvent(PlaylistsContract.Event.NavigateToLiked) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionChip(
                        title = stringResource(R.string.home_quick_action_recent),
                        icon = Icons.Default.History,
                        onClick = { viewModel.handleEvent(PlaylistsContract.Event.NavigateToRecent) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.playlists_section_global))
            }
            item {
                PlaylistGrid(playlists = systemPlaylists) { playlist ->
                    viewModel.handleEvent(PlaylistsContract.Event.PlaylistClicked(playlist))
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.playlists_section_mine))
            }
            item {
                PlaylistGrid(playlists = userPlaylists) { playlist ->
                    viewModel.handleEvent(PlaylistsContract.Event.PlaylistClicked(playlist))
                }
            }
        }
    }

    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.handleEvent(PlaylistsContract.Event.HideCreateDialog) },
            title = { Text(stringResource(R.string.playlists_create_title)) },
            text = {
                OutlinedTextField(
                    value = state.createTitle,
                    onValueChange = {
                        viewModel.handleEvent(PlaylistsContract.Event.CreateTitleChanged(it))
                    },
                    label = { Text(stringResource(R.string.playlists_create_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.handleEvent(PlaylistsContract.Event.CreatePlaylist) },
                    enabled = state.createTitle.isNotBlank() && !state.isCreating,
                ) {
                    Text(stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleEvent(PlaylistsContract.Event.HideCreateDialog) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlaylistGrid(
    playlists: LazyPagingItems<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val items = playlists.itemSnapshotList.items
    if (items.isEmpty()) {
        Text(
            text = stringResource(R.string.playlists_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(spacing.sm),
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier.height(spacing.xxl * 4),
        userScrollEnabled = false,
    ) {
        items(items, key = { it.id }) { playlist ->
            PlaylistCard(
                title = playlist.title,
                songCount = playlist.songCount,
                imageUrl = playlist.coverUrl.ifBlank { null },
                onClick = { onPlaylistClick(playlist) },
            )
        }
    }
}
