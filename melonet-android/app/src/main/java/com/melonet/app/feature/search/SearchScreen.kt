package com.melonet.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloFilterChip
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.MeloSearchBar
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Artist
import com.melonet.app.data.model.SearchFilter
import com.melonet.app.data.model.SearchResultItem
import com.melonet.app.data.model.SearchUser
import com.melonet.app.data.model.Song

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPlaySong: (Song) -> Unit,
    onNavigateToArtist: (Int) -> Unit,
    onNavigateToUser: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val spacing = MeloNetTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchContract.Effect.PlaySong -> onPlaySong(effect.song)
                is SearchContract.Effect.NavigateToArtist -> onNavigateToArtist(effect.artistId)
                is SearchContract.Effect.NavigateToUser -> onNavigateToUser(effect.userId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        MeloSearchBar(
            query = state.query,
            onQueryChange = { viewModel.handleEvent(SearchContract.Event.QueryChanged(it)) },
            onSearch = { viewModel.handleEvent(SearchContract.Event.QuerySubmitted(it)) },
            placeholder = stringResource(R.string.search_hint),
            modifier = Modifier.padding(top = spacing.sm),
        )

        SearchFilterRow(
            selectedFilter = state.selectedFilter,
            onFilterSelected = { filter ->
                viewModel.handleEvent(SearchContract.Event.FilterSelected(filter))
            },
        )

        when {
            state.query.isBlank() -> {
                SearchHistorySection(
                    history = state.history,
                    onHistoryClick = { query ->
                        viewModel.handleEvent(SearchContract.Event.HistoryItemClicked(query))
                    },
                    onHistoryDelete = { query ->
                        viewModel.handleEvent(SearchContract.Event.HistoryItemDeleted(query))
                    },
                )
            }
            searchResults.loadState.refresh is LoadState.Loading && searchResults.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            searchResults.loadState.refresh is LoadState.Error && searchResults.itemCount == 0 -> {
                val errorMessage = (searchResults.loadState.refresh as LoadState.Error)
                    .error.message
                    ?: stringResource(R.string.search_error_title)
                ErrorState(
                    message = errorMessage,
                    onRetry = { searchResults.retry() },
                )
            }
            searchResults.itemCount == 0 &&
                searchResults.loadState.refresh is LoadState.NotLoading -> {
                EmptyState(title = stringResource(R.string.search_no_results))
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    items(
                        count = searchResults.itemCount,
                        key = searchResults.itemKey { it.stableKey() },
                    ) { index ->
                        val item = searchResults[index] ?: return@items
                        SearchResultRow(
                            item = item,
                            onClick = {
                                viewModel.handleEvent(SearchContract.Event.ResultClicked(item))
                            },
                        )
                    }

                    if (searchResults.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(spacing.lg),
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
private fun SearchFilterRow(
    selectedFilter: SearchFilter,
    onFilterSelected: (SearchFilter) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val filters = listOf(
        SearchFilter.ALL to R.string.search_filter_all,
        SearchFilter.SONG to R.string.search_filter_songs,
        SearchFilter.ARTIST to R.string.search_filter_artists,
        SearchFilter.USER to R.string.search_filter_users,
    )

    LazyRow(
        modifier = Modifier.padding(vertical = spacing.sm),
        contentPadding = PaddingValues(horizontal = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(filters.size) { index ->
            val (filter, labelRes) = filters[index]
            MeloFilterChip(
                label = stringResource(labelRes),
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit,
) {
    val spacing = MeloNetTheme.spacing

    if (history.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.search_history_empty_title),
            description = stringResource(R.string.search_history_empty_description),
            icon = Icons.Default.History,
        )
        return
    }

    Column(modifier = Modifier.padding(horizontal = spacing.md)) {
        Text(
            text = stringResource(R.string.search_history_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = spacing.sm),
        )
        history.forEach { query ->
            SearchHistoryItem(
                query = query,
                onClick = { onHistoryClick(query) },
                onDelete = { onHistoryDelete(query) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchHistoryItem(
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = MeloNetTheme.spacing.lg),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_history),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )
    }
}

@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    onClick: () -> Unit,
) {
    when (item) {
        is SearchResultItem.SongItem -> SongResultRow(song = item.song, onClick = onClick)
        is SearchResultItem.ArtistItem -> ArtistResultRow(artist = item.artist, onClick = onClick)
        is SearchResultItem.UserItem -> UserResultRow(user = item.user, onClick = onClick)
    }
}

@Composable
private fun SongResultRow(
    song: Song,
    onClick: () -> Unit,
) {
    val dimensions = MeloNetTheme.dimensions

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            MeloImage(
                imageUrl = song.coverUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(dimensions.avatarSm)
                    .clip(MaterialTheme.shapes.small),
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun ArtistResultRow(
    artist: Artist,
    onClick: () -> Unit,
) {
    val dimensions = MeloNetTheme.dimensions

    ListItem(
        headlineContent = {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            if (artist.songCount > 0) {
                Text(
                    text = stringResource(R.string.search_artist_song_count, artist.songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = {
            MeloImage(
                imageUrl = artist.imageUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(dimensions.avatarSm)
                    .clip(CircleShape),
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun UserResultRow(
    user: SearchUser,
    onClick: () -> Unit,
) {
    val dimensions = MeloNetTheme.dimensions

    ListItem(
        headlineContent = {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.search_user_username, user.username),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            MeloImage(
                imageUrl = user.avatarUrl,
                contentDescription = user.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(dimensions.avatarSm)
                    .clip(CircleShape),
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

private fun SearchResultItem.stableKey(): String = when (this) {
    is SearchResultItem.SongItem -> "song_${song.id}"
    is SearchResultItem.ArtistItem -> "artist_${artist.id}"
    is SearchResultItem.UserItem -> "user_${user.id}"
}
