package com.melonet.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.MeloCard
import com.melonet.app.core.designsystem.component.PremiumSubscriptionCard
import com.melonet.app.core.designsystem.component.ProfileAvatar
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditProfileClick: () -> Unit = {},
    onLikedSongsClick: () -> Unit = {},
    onMyPlaylistsClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    onRecentlyPlayedClick: () -> Unit = {},
    onLocalMusicClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
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
                ProfileContract.Effect.NavigateToRecentlyPlayed -> onRecentlyPlayedClick()
                ProfileContract.Effect.NavigateToLocalMusic -> onLocalMusicClick()
                ProfileContract.Effect.NavigateToDownloads -> onDownloadsClick()
                is ProfileContract.Effect.ShowError -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.lg),
    ) {
        ProfileHeader(
            userName = state.userName.ifBlank { stringResource(R.string.profile_guest_user) },
            avatarUrl = state.avatarUrl,
            isPremium = state.isPremium,
            onEditClick = { viewModel.handleEvent(ProfileContract.Event.EditProfileClicked) },
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        Text(
            text = stringResource(R.string.profile_library_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = spacing.sm),
        )

        MeloCard(onClick = null, modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileLibraryRow(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.profile_liked_songs),
                    onClick = { viewModel.handleEvent(ProfileContract.Event.LikedSongsClicked) },
                )
                ProfileLibraryRow(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = stringResource(R.string.profile_my_playlists),
                    onClick = { viewModel.handleEvent(ProfileContract.Event.MyPlaylistsClicked) },
                )
                ProfileLibraryRow(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.profile_recently_played),
                    onClick = { viewModel.handleEvent(ProfileContract.Event.RecentlyPlayedClicked) },
                )
                ProfileLibraryRow(
                    icon = Icons.Default.People,
                    title = stringResource(R.string.profile_following),
                    onClick = { viewModel.handleEvent(ProfileContract.Event.FollowingClicked) },
                )
                ProfileLibraryRow(
                    icon = Icons.Default.LibraryMusic,
                    title = stringResource(R.string.profile_local_music),
                    onClick = { viewModel.handleEvent(ProfileContract.Event.LocalMusicClicked) },
                )
                ProfileLibraryRow(
                    icon = Icons.Default.Download,
                    title = stringResource(R.string.profile_downloads),
                    onClick = { viewModel.handleEvent(ProfileContract.Event.DownloadsClicked) },
                    showDivider = false,
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        PremiumSubscriptionCard(
            isPremium = state.isPremium,
            onActionClick = {
                viewModel.handleEvent(ProfileContract.Event.UpgradePremiumClicked)
                onUpgradePremiumClick()
            },
        )
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    avatarUrl: String,
    isPremium: Boolean,
    onEditClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val colors = MeloNetTheme.colors

    MeloCard(
        onClick = null,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar(
                avatarUrl = avatarUrl,
                isPremium = isPremium,
                onEditClick = onEditClick,
            )

            Spacer(modifier = Modifier.height(spacing.md))

            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (isPremium) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.premiumContainer)
                        .padding(horizontal = spacing.sm, vertical = spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = colors.premium,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text(
                        text = stringResource(R.string.profile_premium_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPremiumContainer,
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            MeloButton(
                text = stringResource(R.string.profile_edit),
                onClick = onEditClick,
                variant = MeloButtonVariant.Outlined,
            )
        }
    }
}

@Composable
private fun ProfileLibraryRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    val spacing = MeloNetTheme.spacing

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = spacing.md + 40.dp + spacing.md),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
