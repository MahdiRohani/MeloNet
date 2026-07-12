package com.melonet.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.PremiumSubscriptionCard
import com.melonet.app.core.designsystem.component.ProfileAvatar
import com.melonet.app.core.designsystem.component.ProfileQuickActionCard
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditProfileClick: () -> Unit = {},
    onLikedSongsClick: () -> Unit = {},
    onMyPlaylistsClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onUpgradePremiumClick: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProfileContract.Effect.NavigateToEditProfile -> onEditProfileClick()
                ProfileContract.Effect.NavigateToLikedSongs -> onLikedSongsClick()
                ProfileContract.Effect.NavigateToMyPlaylists -> onMyPlaylistsClick()
                ProfileContract.Effect.NavigateToFollowing -> onFollowingClick()
                is ProfileContract.Effect.ShowError -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(spacing.lg))

        ProfileAvatar(
            avatarUrl = state.avatarUrl,
            isPremium = state.isPremium,
            onEditClick = { viewModel.handleEvent(ProfileContract.Event.EditProfileClicked) },
        )

        Spacer(modifier = Modifier.height(spacing.md))

        Text(
            text = state.userName.ifBlank { stringResource(R.string.profile_guest_user) },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            ProfileQuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.profile_liked_songs),
                icon = Icons.Default.Favorite,
                onClick = { viewModel.handleEvent(ProfileContract.Event.LikedSongsClicked) },
            )
            ProfileQuickActionCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.profile_my_playlists),
                icon = Icons.Default.LibraryMusic,
                onClick = { viewModel.handleEvent(ProfileContract.Event.MyPlaylistsClicked) },
            )
        }

        Spacer(modifier = Modifier.height(spacing.md))

        ProfileQuickActionCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.home_quick_action_following),
            icon = Icons.Default.People,
            onClick = { viewModel.handleEvent(ProfileContract.Event.FollowingClicked) },
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        PremiumSubscriptionCard(
            isPremium = state.isPremium,
            onActionClick = {
                viewModel.handleEvent(ProfileContract.Event.UpgradePremiumClicked)
                onUpgradePremiumClick()
            },
        )
    }
}
