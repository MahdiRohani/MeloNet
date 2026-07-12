package com.melonet.app.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.melonet.app.R
import com.melonet.app.core.common.displayMessage
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.PlaylistCard
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Int,
    viewModel: UserProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFollowers: (Int) -> Unit,
    onNavigateToFollowing: (Int) -> Unit,
    onNavigateToPlaylist: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val context = LocalContext.current

    LaunchedEffect(userId) {
        viewModel.handleEvent(UserProfileContract.Event.Load(userId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserProfileContract.Effect.NavigateToFollowers -> onNavigateToFollowers(effect.userId)
                is UserProfileContract.Effect.NavigateToFollowing -> onNavigateToFollowing(effect.userId)
                is UserProfileContract.Effect.NavigateToPlaylist -> onNavigateToPlaylist(effect.playlistId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.user_profile_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                    )
                }
            },
        )

        when {
            state.isLoading && state.user == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.error != null && state.user == null -> {
                ErrorState(
                    message = state.error!!.displayMessage(context),
                    onRetry = { viewModel.handleEvent(UserProfileContract.Event.Load(userId)) },
                )
            }
            state.user != null -> {
                val user = state.user!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MeloImage(
                                imageUrl = user.avatarUrl.ifBlank { null },
                                contentDescription = user.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(MeloNetTheme.dimensions.avatarLg)
                                    .clip(CircleShape),
                            )
                            Spacer(modifier = Modifier.height(spacing.sm))
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = stringResource(R.string.search_user_username, user.username),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (user.bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(spacing.sm))
                                Text(
                                    text = user.bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(modifier = Modifier.height(spacing.md))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                StatChip(
                                    label = stringResource(R.string.social_followers),
                                    count = user.followerCount,
                                    onClick = { viewModel.handleEvent(UserProfileContract.Event.FollowersClicked) },
                                )
                                StatChip(
                                    label = stringResource(R.string.social_following),
                                    count = user.followingCount,
                                    onClick = { viewModel.handleEvent(UserProfileContract.Event.FollowingClicked) },
                                )
                            }
                            if (!user.isSelf) {
                                Spacer(modifier = Modifier.height(spacing.md))
                                MeloButton(
                                    text = stringResource(
                                        if (user.isFollowing) R.string.social_unfollow else R.string.social_follow,
                                    ),
                                    onClick = { viewModel.handleEvent(UserProfileContract.Event.ToggleFollow) },
                                    enabled = !state.isFollowLoading,
                                    variant = if (user.isFollowing) MeloButtonVariant.Outlined else MeloButtonVariant.Primary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    if (state.playlists.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.social_public_playlists),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(state.playlists, key = { it.id }) { playlist ->
                            PlaylistCard(
                                title = playlist.title,
                                songCount = playlist.songCount,
                                imageUrl = playlist.coverUrl.ifBlank { null },
                                onClick = {
                                    viewModel.handleEvent(UserProfileContract.Event.PlaylistClicked(playlist.id))
                                },
                            )
                        }
                    } else if (!state.isLoading) {
                        item {
                            EmptyState(title = stringResource(R.string.social_no_playlists))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    count: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleLarge)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
