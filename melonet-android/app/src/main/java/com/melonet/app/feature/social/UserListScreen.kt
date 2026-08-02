package com.melonet.app.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.common.displayMessage
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.SocialUser
import com.melonet.app.data.model.UserListType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    userId: Int,
    listType: UserListType,
    title: String,
    viewModel: UserListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUser: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userId, listType) {
        viewModel.handleEvent(UserListContract.Event.Load(userId, listType))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserListContract.Effect.NavigateToUser -> onNavigateToUser(effect.userId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
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
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            state.error != null && state.users.isEmpty() -> {
                ErrorState(
                    message = state.error!!.displayMessage(context),
                    onRetry = { viewModel.handleEvent(UserListContract.Event.Load(userId, listType)) },
                )
            }
            state.users.isEmpty() -> {
                EmptyState(title = stringResource(R.string.social_empty_list))
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.users, key = { it.id }) { user ->
                        SocialUserRow(
                            user = user,
                            onClick = { viewModel.handleEvent(UserListContract.Event.UserClicked(user.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialUserRow(
    user: SocialUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    largeAvatar: Boolean = false,
) {
    val dimensions = MeloNetTheme.dimensions
    val spacing = MeloNetTheme.spacing
    val avatarSize = if (largeAvatar) 56.dp else dimensions.avatarSm
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = user.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
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
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
            )
        },
    )
}
