package com.melonet.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.MeloImageFallback
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.SearchUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    viewModel: NewChatViewModel,
    onNavigateBack: () -> Unit,
    onOpenChat: (userId: Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NewChatContract.Effect.OpenChat -> onOpenChat(effect.userId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.chat_new_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.handleEvent(NewChatContract.Event.QueryChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            placeholder = { Text(stringResource(R.string.chat_search_users_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = CircleShape,
        )

        when {
            state.isSearching -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.results.isEmpty() && state.hasSearched -> {
                EmptyState(title = stringResource(R.string.chat_search_no_users))
            }
            state.results.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.chat_new_empty_title),
                    description = stringResource(R.string.chat_new_empty_description),
                    icon = Icons.Default.Search,
                )
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(vertical = spacing.sm)) {
                    items(state.results, key = { it.id }) { user ->
                        NewChatUserRow(
                            user = user,
                            onClick = {
                                viewModel.handleEvent(NewChatContract.Event.UserClicked(user))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatUserRow(user: SearchUser, onClick: () -> Unit) {
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
                fallback = MeloImageFallback.Person,
                modifier = Modifier
                    .size(dimensions.avatarSm)
                    .clip(CircleShape),
            )
        },
    )
}
