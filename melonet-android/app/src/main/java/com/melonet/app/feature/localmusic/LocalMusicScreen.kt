package com.melonet.app.feature.localmusic

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.permission.hasAudioReadPermission
import com.melonet.app.data.model.Song
import com.melonet.app.feature.playlists.SongListItem
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun LocalMusicScreen(
    viewModel: LocalMusicViewModel,
    onPlaySong: (Song, List<Song>) -> Unit,
    requestAudioPermission: (onResult: (Boolean) -> Unit) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (context.hasAudioReadPermission()) {
            viewModel.handleEvent(LocalMusicContract.Event.PermissionGranted)
        } else {
            val granted = suspendCancellableCoroutine { continuation ->
                requestAudioPermission { continuation.resume(it) }
            }
            if (granted) {
                viewModel.handleEvent(LocalMusicContract.Event.PermissionGranted)
            } else {
                viewModel.handleEvent(LocalMusicContract.Event.PermissionDenied)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LocalMusicContract.Effect.PlaySong -> onPlaySong(effect.song, effect.queue)
            }
        }
    }

    when {
        !state.hasPermission && state.permissionRequested -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.local_music_permission_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = stringResource(R.string.local_music_permission_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(spacing.lg))
                MeloButton(
                    text = stringResource(R.string.local_music_grant),
                    onClick = {
                        requestAudioPermission { granted ->
                            if (granted) {
                                viewModel.handleEvent(LocalMusicContract.Event.PermissionGranted)
                            } else {
                                viewModel.handleEvent(LocalMusicContract.Event.PermissionDenied)
                            }
                        }
                    },
                )
            }
        }
        state.isLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.local_music_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = spacing.md),
                )

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = {
                        viewModel.handleEvent(LocalMusicContract.Event.SearchQueryChanged(it))
                    },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                Text(
                    text = stringResource(R.string.local_music_song_count, state.filteredSongs.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.filteredSongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        MeloButton(
                            text = stringResource(R.string.library_play_all),
                            onClick = { viewModel.handleEvent(LocalMusicContract.Event.PlayAll) },
                            modifier = Modifier.weight(1f),
                        )
                        MeloButton(
                            text = stringResource(R.string.library_shuffle),
                            onClick = { viewModel.handleEvent(LocalMusicContract.Event.ShuffleAll) },
                            modifier = Modifier.weight(1f),
                            variant = MeloButtonVariant.Outlined,
                        )
                    }
                }

                if (state.filteredSongs.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.local_music_empty_title),
                        description = stringResource(R.string.local_music_empty_description),
                        modifier = Modifier.padding(top = spacing.lg),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        items(state.filteredSongs, key = { it.id }) { song ->
                            SongListItem(
                                song = song,
                                onClick = {
                                    viewModel.handleEvent(LocalMusicContract.Event.SongClicked(song))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
