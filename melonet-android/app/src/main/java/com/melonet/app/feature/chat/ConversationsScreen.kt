package com.melonet.app.feature.chat

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.melonet.app.R
import com.melonet.app.core.common.displayMessage
import com.melonet.app.core.designsystem.component.ChatConnectionBanner
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.MeloSearchBar
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.network.toAppError
import com.melonet.app.data.model.Conversation
import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.model.MessageType
import com.melonet.app.data.repository.ChatRepository
import org.koin.compose.koinInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Int, Int, String) -> Unit,
    onNewChat: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val conversations = viewModel.conversations.collectAsLazyPagingItems()
    val spacing = MeloNetTheme.spacing
    val context = LocalContext.current
    val chatRepository: ChatRepository = koinInject()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.handleEvent(ConversationsContract.Event.ScreenVisible)
    }

    LaunchedEffect(state.refreshKey) {
        if (state.refreshKey > 0) {
            conversations.refresh()
        }
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
                ConversationsContract.Effect.RefreshList -> conversations.refresh()
            }
        }
    }

    val filteredIndices = remember(conversations.itemCount, state.searchQuery, conversations.itemSnapshotList) {
        val query = state.searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            (0 until conversations.itemCount).toList()
        } else {
            (0 until conversations.itemCount).mapNotNull { index ->
                val conversation = conversations.peek(index) ?: return@mapNotNull null
                val haystack = buildString {
                    append(conversation.otherUser.displayName)
                    append(' ')
                    append(conversation.otherUser.username)
                    append(' ')
                    append(conversation.lastMessage?.content.orEmpty())
                }.lowercase()
                if (haystack.contains(query)) index else null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.chat_conversations_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            actions = {
                IconButton(onClick = onNewChat) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.chat_new_title),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background),
        )

        ChatConnectionBanner(
            state = state.connectionState,
            onRetryConnect = { chatRepository.connect() },
        )

        MeloSearchBar(
            query = state.searchQuery,
            onQueryChange = {
                viewModel.handleEvent(ConversationsContract.Event.SearchChanged(it))
            },
            placeholder = stringResource(R.string.chat_search_conversations_hint),
            modifier = Modifier.padding(vertical = spacing.xs),
        )

        when (conversations.loadState.refresh) {
            is LoadState.Loading if conversations.itemCount == 0 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is LoadState.Error if conversations.itemCount == 0 -> {
                val errorMessage = (conversations.loadState.refresh as LoadState.Error)
                    .error
                    .toAppError()
                    .displayMessage(context)
                ErrorState(
                    message = errorMessage,
                    onRetry = { conversations.retry() },
                )
            }
            else -> {
                if (filteredIndices.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.chat_empty_title),
                        description = stringResource(R.string.chat_empty_description),
                        icon = Icons.AutoMirrored.Filled.Chat,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = spacing.xs),
                    ) {
                        items(
                            count = filteredIndices.size,
                            key = { pos ->
                                val conversation = conversations.peek(filteredIndices[pos])
                                conversation?.id ?: "idx_$pos"
                            },
                        ) { pos ->
                            val conversation = conversations[filteredIndices[pos]] ?: return@items
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
    val scheme = MaterialTheme.colorScheme
    val lastMessage = conversation.lastMessage
    val preview = lastMessage?.let { message ->
        when (message.msgType) {
            MessageType.SONG -> stringResource(R.string.chat_song_preview)
            else -> message.content
        }
    }.orEmpty()
    val hasUnread = conversation.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            MeloImage(
                imageUrl = conversation.otherUser.avatarUrl,
                contentDescription = conversation.otherUser.displayName,
                contentScale = ContentScale.Crop,
                targetSize = 56.dp,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            )
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(scheme.background)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(scheme.primary),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.otherUser.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = scheme.onBackground,
                )
                Text(
                    text = formatConversationTime(conversation.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) scheme.primary else scheme.onSurfaceVariant,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (lastMessage?.isMine == true) {
                    ConversationReceiptIcon(status = lastMessage.status)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = preview.ifBlank { "…" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasUnread) scheme.onBackground else scheme.onSurfaceVariant,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (hasUnread) {
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(
                        text = conversation.unreadCount.coerceAtMost(99).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimary,
                        modifier = Modifier
                            .background(color = scheme.primary, shape = CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationReceiptIcon(status: MessageStatus) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    when (status) {
        MessageStatus.FAILED -> Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        MessageStatus.PENDING -> CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 1.5.dp,
            color = tint,
        )
        MessageStatus.SENT -> Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint,
        )
        MessageStatus.DELIVERED, MessageStatus.READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (status == MessageStatus.READ) {
                MaterialTheme.colorScheme.primary
            } else {
                tint
            },
        )
    }
}

private fun formatConversationTime(instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val day = instant.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return if (ChronoUnit.DAYS.between(day, today) == 0L) {
        DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(instant)
    } else {
        DateTimeFormatter.ofPattern("MMM d").withZone(zone).format(instant)
    }
}
