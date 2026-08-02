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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.AnimateEnter
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.PlaylistCard
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
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlaylistsContract.Effect.NavigateToDetail -> onNavigateToDetail(effect.playlistId)
                PlaylistsContract.Effect.NavigateToLiked -> onNavigateToLiked()
                PlaylistsContract.Effect.NavigateToRecent -> onNavigateToRecent()
                is PlaylistsContract.Effect.ShowError -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = spacing.md,
                bottom = spacing.xxl * 2,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item(key = "header") {
                AnimateEnter {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.playlists_library_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onBackground,
                        )
                        MeloButton(
                            text = stringResource(R.string.action_create),
                            onClick = { viewModel.handleEvent(PlaylistsContract.Event.ShowCreateDialog) },
                            variant = MeloButtonVariant.Secondary,
                        )
                    }
                }
            }

            item(key = "shortcuts") {
                AnimateEnter(delayMillis = 40) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        LibraryShortcutCard(
                            title = stringResource(R.string.profile_liked_songs),
                            subtitle = stringResource(R.string.playlists_liked_subtitle),
                            icon = Icons.Default.Favorite,
                            colors = listOf(
                                scheme.primary.copy(alpha = 0.55f),
                                scheme.tertiary.copy(alpha = 0.35f),
                            ),
                            onClick = { viewModel.handleEvent(PlaylistsContract.Event.NavigateToLiked) },
                            modifier = Modifier.weight(1f),
                        )
                        LibraryShortcutCard(
                            title = stringResource(R.string.home_quick_action_recent),
                            subtitle = stringResource(R.string.playlists_recent_subtitle),
                            icon = Icons.Default.History,
                            colors = listOf(
                                scheme.secondary.copy(alpha = 0.5f),
                                scheme.primary.copy(alpha = 0.28f),
                            ),
                            onClick = { viewModel.handleEvent(PlaylistsContract.Event.NavigateToRecent) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item(key = "mine_header") {
                AnimateEnter(delayMillis = 80) {
                    SectionHeader(title = stringResource(R.string.playlists_section_mine))
                }
            }
            item(key = "mine_list") {
                AnimateEnter(delayMillis = 100) {
                    PlaylistList(playlists = userPlaylists) { playlist ->
                        viewModel.handleEvent(PlaylistsContract.Event.PlaylistClicked(playlist))
                    }
                }
            }

            item(key = "featured_header") {
                AnimateEnter(delayMillis = 120) {
                    SectionHeader(title = stringResource(R.string.playlists_section_featured))
                }
            }
            item(key = "featured_grid") {
                AnimateEnter(delayMillis = 140) {
                    PlaylistGrid(playlists = systemPlaylists) { playlist ->
                        viewModel.handleEvent(PlaylistsContract.Event.PlaylistClicked(playlist))
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.handleEvent(PlaylistsContract.Event.HideCreateDialog) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
                    .padding(bottom = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.playlists_create_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.playlists_create_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.createTitle,
                    onValueChange = {
                        viewModel.handleEvent(PlaylistsContract.Event.CreateTitleChanged(it))
                    },
                    label = { Text(stringResource(R.string.playlists_create_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    MeloButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = { viewModel.handleEvent(PlaylistsContract.Event.HideCreateDialog) },
                        variant = MeloButtonVariant.Outlined,
                        modifier = Modifier.weight(1f),
                    )
                    MeloButton(
                        text = stringResource(R.string.action_create),
                        onClick = { viewModel.handleEvent(PlaylistsContract.Event.CreatePlaylist) },
                        enabled = state.createTitle.isNotBlank() && !state.isCreating,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    Box(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: LazyPagingItems<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val items = playlists.itemSnapshotList.items
    if (items.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.playlists_empty_mine),
            description = stringResource(R.string.playlists_empty_mine_description),
            icon = Icons.Default.Add,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        items.forEach { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.45f))
                    .clickable { onPlaylistClick(playlist) }
                    .padding(horizontal = spacing.sm, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                MeloImage(
                    imageUrl = playlist.coverUrl.ifBlank { null },
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = scheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.playlists_song_count, playlist.songCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
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
        EmptyState(
            title = stringResource(R.string.playlists_empty),
            description = stringResource(R.string.playlists_empty_description),
            icon = Icons.Default.Add,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        )
        return
    }
    val rows = (items.size + 1) / 2
    val gridHeight = (rows * 210).dp + ((rows - 1).coerceAtLeast(0) * spacing.sm.value).dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight.coerceAtLeast(210.dp)),
        userScrollEnabled = false,
    ) {
        items(items, key = { it.id }) { playlist ->
            PlaylistCard(
                title = playlist.title,
                songCount = playlist.songCount,
                imageUrl = playlist.coverUrl.ifBlank { null },
                onClick = { onPlaylistClick(playlist) },
                large = true,
            )
        }
    }
}
