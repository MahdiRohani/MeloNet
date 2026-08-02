package com.melonet.app.feature.localmusic

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.AnimateEnter
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.MeloSearchBar
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.permission.hasAudioReadPermission
import com.melonet.app.core.ui.rememberMeloHaptics
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
    val haptics = rememberMeloHaptics()

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
                AnimateEnter {
                    Text(
                        text = stringResource(R.string.local_music_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = spacing.md),
                    )
                }

                MeloSearchBar(
                    query = state.searchQuery,
                    onQueryChange = {
                        viewModel.handleEvent(LocalMusicContract.Event.SearchQueryChanged(it))
                    },
                    placeholder = stringResource(R.string.search_hint),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                ScrollableTabRow(
                    selectedTabIndex = state.selectedTab.ordinal,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    LocalMusicTab.entries.forEach { tab ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = {
                                haptics.tick()
                                viewModel.handleEvent(LocalMusicContract.Event.TabSelected(tab))
                            },
                            text = {
                                Text(
                                    text = when (tab) {
                                        LocalMusicTab.Songs -> stringResource(R.string.local_music_tab_songs)
                                        LocalMusicTab.Albums -> stringResource(R.string.local_music_tab_albums)
                                        LocalMusicTab.Artists -> stringResource(R.string.local_music_tab_artists)
                                    },
                                )
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.sm))

                val countLabel = when (state.selectedTab) {
                    LocalMusicTab.Songs -> stringResource(R.string.local_music_song_count, state.filteredSongs.size)
                    LocalMusicTab.Albums -> stringResource(R.string.local_music_album_count, state.albumGroups.size)
                    LocalMusicTab.Artists -> stringResource(R.string.local_music_artist_count, state.artistGroups.size)
                }
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.filteredSongs.isNotEmpty() && state.selectedTab == LocalMusicTab.Songs) {
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

                when (state.selectedTab) {
                    LocalMusicTab.Songs -> {
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
                                itemsIndexed(state.filteredSongs, key = { _, song -> song.id }) { index, song ->
                                    AnimateEnter(delayMillis = (index * 20).coerceAtMost(160)) {
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
                    LocalMusicTab.Albums, LocalMusicTab.Artists -> {
                        val groups = if (state.selectedTab == LocalMusicTab.Albums) {
                            state.albumGroups
                        } else {
                            state.artistGroups
                        }
                        if (groups.isEmpty()) {
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
                                items(groups, key = { it.key }) { group ->
                                    LocalGroupRow(
                                        group = group,
                                        onClick = {
                                            viewModel.handleEvent(LocalMusicContract.Event.GroupClicked(group))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalGroupRow(
    group: LocalMusicGroup,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        headlineContent = { Text(group.title) },
        supportingContent = {
            val meta = buildString {
                if (group.subtitle.isNotBlank()) {
                    append(group.subtitle)
                    append(" · ")
                }
                append(stringResource(R.string.local_music_tracks_in_group, group.songs.size))
            }
            Text(meta)
        },
        leadingContent = {
            MeloImage(
                imageUrl = group.coverUrl,
                contentDescription = group.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
