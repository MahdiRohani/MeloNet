package com.melonet.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Conversation
import com.melonet.app.data.model.MessageType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Int, Int, String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val conversations = viewModel.conversations.collectAsLazyPagingItems()
    val spacing = MeloNetTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.handleEvent(ConversationsContract.Event.ScreenVisible)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ConversationsContract.Effect.NavigateToChat -> {
                    onNavigateToChat(
                        effect.conversationId,
                        effect.otherUserId,
                        effect.otherDisplayName,
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.chat_conversations_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            },
        )

        when (conversations.loadState.refresh) {
            is LoadState.Loading if conversations.itemCount == 0 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is LoadState.Error if conversations.itemCount == 0 -> {
                ErrorState(
                    message = stringResource(R.string.chat_error_title),
                    onRetry = { conversations.retry() },
                )
            }
            else -> {
                if (conversations.itemCount == 0) {
                    EmptyState(
                        title = stringResource(R.string.chat_empty_title),
                        description = stringResource(R.string.chat_empty_description),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = spacing.sm),
                    ) {
                        items(
                            count = conversations.itemCount,
                            key = conversations.itemKey { it.id },
                        ) { index ->
                            val conversation = conversations[index] ?: return@items
                            ConversationRow(
                                conversation = conversation,
                                onClick = {
                                    viewModel.handleEvent(
                                        ConversationsContract.Event.ConversationClicked(conversation),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val preview = conversation.lastMessage?.let { message ->
        when (message.msgType) {
            MessageType.SONG -> stringResource(R.string.chat_song_preview)
            else -> message.content
        }
    }.orEmpty()

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        leadingContent = {
            MeloImage(
                imageUrl = conversation.otherUser.avatarUrl,
                contentDescription = conversation.otherUser.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(spacing.xxl)
                    .clip(CircleShape),
            )
        },
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.otherUser.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatConversationTime(conversation.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        supportingContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (conversation.unreadCount > 0) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            )
                            .padding(horizontal = spacing.sm, vertical = spacing.xs),
                    )
                }
            }
        },
    )
}

private fun formatConversationTime(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
