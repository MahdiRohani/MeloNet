package com.melonet.app.feature.following

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.ArtistCircleItem
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.SearchUser
import com.melonet.app.feature.social.SocialUserRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    userId: Int,
    viewModel: FollowingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUser: (Int) -> Unit,
    onNavigateToArtist: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val spacing = MeloNetTheme.spacing

    LaunchedEffect(userId) {
        viewModel.handleEvent(FollowingContract.Event.Load(userId))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.home_quick_action_following)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                    )
                }
            },
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.handleEvent(FollowingContract.Event.QueryChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            placeholder = { Text(stringResource(R.string.following_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.following_tab_people)) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.following_tab_artists)) },
            )
        }

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            selectedTab == 0 -> {
                val filtered = state.filteredUsers
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (filtered.isEmpty() && state.query.isBlank()) {
                        item {
                            EmptyState(title = stringResource(R.string.following_empty_people))
                        }
                    } else if (filtered.isEmpty() && state.searchResults.isEmpty() && !state.isSearchingUsers) {
                        item {
                            EmptyState(title = stringResource(R.string.chat_search_no_users))
                        }
                    } else {
                        items(filtered, key = { "f_${it.id}" }) { user ->
                            SocialUserRow(user = user, onClick = { onNavigateToUser(user.id) })
                        }
                    }

                    if (state.searchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.chat_new_empty_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                            )
                        }
                        items(state.searchResults, key = { "s_${it.id}" }) { user ->
                            DiscoverUserRow(user = user, onClick = { onNavigateToUser(user.id) })
                        }
                    }

                    if (state.isSearchingUsers) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
            else -> {
                val artists = state.filteredArtists
                if (artists.isEmpty()) {
                    EmptyState(title = stringResource(R.string.following_empty_artists))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(MeloNetTheme.spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        gridItems(artists, key = { it.id }) { artist ->
                            ArtistCircleItem(
                                name = artist.name,
                                imageUrl = artist.imageUrl,
                                onClick = { onNavigateToArtist(artist.id) },
                                modifier = Modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverUserRow(user: SearchUser, onClick: () -> Unit) {
    val dimensions = MeloNetTheme.dimensions
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Text(user.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.search_user_username, user.username),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            MeloImage(
                imageUrl = user.avatarUrl.ifBlank { null },
                contentDescription = user.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(dimensions.avatarSm)
                    .clip(CircleShape),
            )
        },
    )
}
