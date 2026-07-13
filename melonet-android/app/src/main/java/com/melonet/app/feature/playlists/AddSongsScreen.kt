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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.permission.hasAudioReadPermission
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsScreen(
    playlistId: Int,
    viewModel: AddSongsViewModel,
    onNavigateBack: () -> Unit,
    requestAudioPermission: (onResult: (Boolean) -> Unit) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val context = LocalContext.current

    LaunchedEffect(playlistId) {
        viewModel.handleEvent(AddSongsContract.Event.Init(playlistId))
        if (context.hasAudioReadPermission()) {
            viewModel.handleEvent(AddSongsContract.Event.LocalPermissionGranted)
        } else {
            val granted = suspendCancellableCoroutine { continuation ->
                requestAudioPermission { continuation.resume(it) }
            }
            if (granted) {
                viewModel.handleEvent(AddSongsContract.Event.LocalPermissionGranted)
            } else {
                viewModel.handleEvent(AddSongsContract.Event.LocalPermissionDenied)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AddSongsContract.Effect.NavigateBack -> onNavigateBack()
                is AddSongsContract.Effect.SongAdded -> { }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_songs_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_player_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                Tab(
                    selected = state.selectedTab == AddSongsContract.Tab.DEVICE,
                    onClick = {
                        viewModel.handleEvent(AddSongsContract.Event.TabSelected(AddSongsContract.Tab.DEVICE))
                    },
                    text = { Text(stringResource(R.string.add_songs_tab_device)) },
                )
                Tab(
                    selected = state.selectedTab == AddSongsContract.Tab.APP,
                    onClick = {
                        viewModel.handleEvent(AddSongsContract.Event.TabSelected(AddSongsContract.Tab.APP))
                    },
                    text = { Text(stringResource(R.string.add_songs_tab_app)) },
                )
            }

            when (state.selectedTab) {
                AddSongsContract.Tab.DEVICE -> DeviceSongsTab(
                    state = state,
                    onAddSong = { viewModel.handleEvent(AddSongsContract.Event.AddSong(it)) },
                )
                AddSongsContract.Tab.APP -> AppSongsTab(
                    state = state,
                    onQueryChange = {
                        viewModel.handleEvent(AddSongsContract.Event.SearchQueryChanged(it))
                    },
                    onAddSong = { viewModel.handleEvent(AddSongsContract.Event.AddSong(it)) },
                )
            }
        }
    }
}

@Composable
private fun DeviceSongsTab(
    state: AddSongsContract.State,
    onAddSong: (Song) -> Unit,
) {
    val spacing = MeloNetTheme.spacing

    when {
        state.isLoadingLocal -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        !state.hasLocalPermission -> {
            EmptyState(
                title = stringResource(R.string.local_music_permission_title),
                description = stringResource(R.string.local_music_permission_message),
            )
        }
        state.filteredLocalSongs.isEmpty() -> {
            EmptyState(
                title = stringResource(R.string.local_music_empty_title),
                description = stringResource(R.string.local_music_empty_description),
            )
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(state.filteredLocalSongs, key = { it.id }) { song ->
                    AddSongListItem(
                        song = song,
                        isAdded = song.id in state.addedSongIds,
                        onAdd = { onAddSong(song) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSongsTab(
    state: AddSongsContract.State,
    onQueryChange: (String) -> Unit,
    onAddSong: (Song) -> Unit,
) {
    val spacing = MeloNetTheme.spacing

    Column(modifier = Modifier.padding(spacing.md)) {
        OutlinedTextField(
            value = state.appSearchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.add_songs_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(spacing.sm))

        when {
            state.isSearching -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.appSearchQuery.isBlank() -> {
                EmptyState(
                    title = stringResource(R.string.add_songs_search_empty),
                )
            }
            state.appSearchResults.isEmpty() -> {
                EmptyState(title = stringResource(R.string.search_no_results))
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    items(state.appSearchResults, key = { it.id }) { song ->
                        AddSongListItem(
                            song = song,
                            isAdded = song.id in state.addedSongIds,
                            onAdd = { onAddSong(song) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSongListItem(
    song: Song,
    isAdded: Boolean,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MeloNetTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        TextButton(
            onClick = onAdd,
            enabled = !isAdded,
        ) {
            Text(
                text = stringResource(
                    if (isAdded) R.string.add_songs_added else R.string.add_songs_add,
                ),
            )
        }
    }
}
