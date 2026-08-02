package com.melonet.app.feature.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.melonet.app.R
import com.melonet.app.core.common.displayMessage
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.MeloCard
import com.melonet.app.core.designsystem.component.PremiumSubscriptionCard
import com.melonet.app.core.designsystem.component.ProfileAvatar
import com.melonet.app.core.designsystem.theme.MeloMotion
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.network.MediaUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val imageLoader = remember { ImageLoader(context) }
    val scheme = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current

    var heroColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    var contentEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentEntered = true }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.avatarUrl) {
        val url = MediaUrl.resolve(state.avatarUrl)
        heroColors = if (url.isNullOrBlank()) {
            emptyList()
        } else {
            extractAvatarPalette(context, imageLoader, url)
        }
    }

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
                is ProfileContract.Effect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.error.displayMessage(context))
                }
            }
        }
    }

    val enterAlpha by animateFloatAsState(
        targetValue = if (contentEntered) 1f else 0f,
        animationSpec = MeloMotion.fadeTween,
        label = "profile_enter_alpha",
    )
    val enterOffset by animateFloatAsState(
        targetValue = if (contentEntered) 0f else 16f,
        animationSpec = MeloMotion.pressSpring,
        label = "profile_enter_offset",
    )

    val gradientColors = if (heroColors.isNotEmpty()) {
        listOf(
            heroColors.first().copy(alpha = 0.28f),
            (heroColors.getOrNull(1) ?: heroColors.first()).copy(alpha = 0.12f),
            scheme.surface.copy(alpha = 0.92f),
            scheme.background,
        )
    } else {
        listOf(
            scheme.primary.copy(alpha = 0.16f),
            scheme.secondary.copy(alpha = 0.08f),
            scheme.surface,
            scheme.background,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = scheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Brush.verticalGradient(gradientColors)),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        alpha = enterAlpha
                        translationY = enterOffset
                    }
                    .padding(bottom = spacing.xl),
            ) {
            ProfileHeroHeader(
                userName = state.userName.ifBlank { stringResource(R.string.profile_guest_user) },
                username = state.username,
                avatarUrl = state.avatarUrl,
                isPremium = state.isPremium,
                onEditClick = { viewModel.handleEvent(ProfileContract.Event.EditProfileClicked) },
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            ProfileStatsRow(
                onFollowingClick = { viewModel.handleEvent(ProfileContract.Event.FollowingClicked) },
                onPlaylistsClick = { viewModel.handleEvent(ProfileContract.Event.MyPlaylistsClicked) },
                onLikedClick = { viewModel.handleEvent(ProfileContract.Event.LikedSongsClicked) },
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = stringResource(R.string.profile_library_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onBackground,
                modifier = Modifier.padding(horizontal = spacing.md),
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            ProfileLibraryGrid(
                onLiked = { viewModel.handleEvent(ProfileContract.Event.LikedSongsClicked) },
                onPlaylists = { viewModel.handleEvent(ProfileContract.Event.MyPlaylistsClicked) },
                onRecent = { viewModel.handleEvent(ProfileContract.Event.RecentlyPlayedClicked) },
                onDownloads = { viewModel.handleEvent(ProfileContract.Event.DownloadsClicked) },
                onLocal = { viewModel.handleEvent(ProfileContract.Event.LocalMusicClicked) },
                onFollowing = { viewModel.handleEvent(ProfileContract.Event.FollowingClicked) },
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            Box(modifier = Modifier.padding(horizontal = spacing.md)) {
                PremiumSubscriptionCard(
                    isPremium = state.isPremium,
                    onActionClick = {
                        viewModel.handleEvent(ProfileContract.Event.UpgradePremiumClicked)
                    },
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            MeloButton(
                text = stringResource(R.string.profile_edit),
                onClick = { viewModel.handleEvent(ProfileContract.Event.EditProfileClicked) },
                variant = MeloButtonVariant.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md),
            )
            }
        }
    }
}

@Composable
private fun ProfileHeroHeader(
    userName: String,
    username: String,
    avatarUrl: String,
    isPremium: Boolean,
    onEditClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val colors = MeloNetTheme.colors
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md)
            .padding(top = spacing.lg),
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
            color = scheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (username.isNotBlank()) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.profile_username, username),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }

        if (isPremium) {
            Spacer(modifier = Modifier.height(spacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.premiumContainer)
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
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
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPremiumContainer,
                )
            }
        }
    }
}

@Composable
private fun ProfileStatsRow(
    onFollowingClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onLikedClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md)
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surface.copy(alpha = 0.72f))
            .padding(vertical = spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProfileStatItem(
            label = stringResource(R.string.profile_stat_following),
            onClick = onFollowingClick,
        )
        ProfileStatItem(
            label = stringResource(R.string.profile_stat_playlists),
            onClick = onPlaylistsClick,
        )
        ProfileStatItem(
            label = stringResource(R.string.profile_stat_liked),
            onClick = onLikedClick,
        )
    }
}

@Composable
private fun ProfileStatItem(
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ProfileLibraryGrid(
    onLiked: () -> Unit,
    onPlaylists: () -> Unit,
    onRecent: () -> Unit,
    onDownloads: () -> Unit,
    onLocal: () -> Unit,
    onFollowing: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val items = listOf(
        LibraryTile(Icons.Default.Favorite, stringResource(R.string.profile_liked_songs), onLiked),
        LibraryTile(Icons.AutoMirrored.Filled.QueueMusic, stringResource(R.string.profile_my_playlists), onPlaylists),
        LibraryTile(Icons.Default.History, stringResource(R.string.profile_recently_played), onRecent),
        LibraryTile(Icons.Default.Download, stringResource(R.string.profile_downloads), onDownloads),
        LibraryTile(Icons.Default.LibraryMusic, stringResource(R.string.profile_local_music), onLocal),
        LibraryTile(Icons.Default.People, stringResource(R.string.profile_following), onFollowing),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                row.forEach { tile ->
                    ProfileLibraryTile(
                        icon = tile.icon,
                        title = tile.title,
                        onClick = tile.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class LibraryTile(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
)

@Composable
private fun ProfileLibraryTile(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme

    MeloCard(
        onClick = onClick,
        modifier = modifier.height(108.dp),
        containerColor = scheme.surface.copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private suspend fun extractAvatarPalette(
    context: android.content.Context,
    imageLoader: ImageLoader,
    url: String,
): List<Color> = withContext(Dispatchers.IO) {
    try {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(128)
            .build()
        val result = imageLoader.execute(request)
        if (result !is SuccessResult) return@withContext emptyList()
        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            ?: return@withContext emptyList()
        val palette = Palette.from(bitmap).generate()
        listOfNotNull(
            palette.darkVibrantSwatch?.rgb,
            palette.vibrantSwatch?.rgb,
            palette.mutedSwatch?.rgb,
            palette.darkMutedSwatch?.rgb,
        ).distinct().take(3).map { Color(it) }
    } catch (_: Exception) {
        emptyList()
    }
}
