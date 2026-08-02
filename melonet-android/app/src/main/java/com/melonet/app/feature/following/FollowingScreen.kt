package com.melonet.app.feature.following

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.ArtistCircleItem
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.MeloImageFallback
import com.melonet.app.core.designsystem.component.MeloSearchBar
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
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(userId) {
        viewModel.handleEvent(FollowingContract.Event.Load(userId))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.primary.copy(alpha = 0.18f),
                            scheme.secondary.copy(alpha = 0.08f),
                            scheme.background,
                        ),
                    ),
                ),
        ) {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.home_quick_action_following),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.following_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_player_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                )
                MeloSearchBar(
                    query = state.query,
                    onQueryChange = { viewModel.handleEvent(FollowingContract.Event.QueryChanged(it)) },
                    placeholder = stringResource(R.string.following_search_hint),
                    modifier = Modifier.padding(bottom = spacing.sm),
                )
            }
        }

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
                    CircularProgressIndicator(color = scheme.primary)
                }
            }
            selectedTab == 0 -> PeopleTab(
                state = state,
                onNavigateToUser = onNavigateToUser,
            )
            else -> ArtistsTab(
                state = state,
                onNavigateToArtist = onNavigateToArtist,
            )
        }
    }
}

@Composable
private fun PeopleTab(
    state: FollowingContract.State,
    onNavigateToUser: (Int) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val filtered = state.filteredUsers

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xl),
    ) {
        if (filtered.isEmpty() && state.query.isBlank() && state.searchResults.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.following_empty_people),
                    description = stringResource(R.string.following_empty_people_description),
                    icon = Icons.Outlined.People,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                )
            }
        } else if (filtered.isEmpty() && state.searchResults.isEmpty() && !state.isSearchingUsers) {
            item {
                EmptyState(
                    title = stringResource(R.string.chat_search_no_users),
                    description = stringResource(R.string.chat_new_empty_description),
                    icon = Icons.Outlined.Search,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }
        } else if (filtered.isNotEmpty()) {
            item {
                SectionLabel(
                    text = stringResource(R.string.following_section_following),
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                )
            }
            items(filtered, key = { "f_${it.id}" }) { user ->
                SocialUserRow(
                    user = user,
                    onClick = { onNavigateToUser(user.id) },
                    largeAvatar = true,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }

        if (state.searchResults.isNotEmpty()) {
            item {
                SectionLabel(
                    text = stringResource(R.string.following_section_discover),
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

@Composable
private fun ArtistsTab(
    state: FollowingContract.State,
    onNavigateToArtist: (Int) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val artists = state.filteredArtists

    if (artists.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.following_empty_artists),
            description = stringResource(R.string.following_empty_artists_description),
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionLabel(
            text = stringResource(R.string.following_section_artists),
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            gridItems(artists, key = { it.id }) { artist ->
                ArtistCircleItem(
                    name = artist.name,
                    imageUrl = artist.imageUrl,
                    onClick = { onNavigateToArtist(artist.id) },
                    size = 104,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun DiscoverUserRow(user: SearchUser, onClick: () -> Unit) {
    val spacing = MeloNetTheme.spacing
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(user.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (user.isPremium) {
                    Text(
                        text = stringResource(R.string.profile_premium_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MeloNetTheme.colors.onPremiumContainer,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MeloNetTheme.colors.premiumContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
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
                fallback = MeloImageFallback.Person,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            )
        },
    )
}
