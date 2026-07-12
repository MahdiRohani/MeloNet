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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.melonet.app.R
import com.melonet.app.core.common.displayMessage
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.model.MessageType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUserId: Int,
    conversationId: Int,
    shareSongId: String?,
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onPlaySong: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val pagingMessages = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val spacing = MeloNetTheme.spacing
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(otherUserId, conversationId) {
        viewModel.handleEvent(ChatContract.Event.Load(otherUserId, conversationId))
    }

    LaunchedEffect(shareSongId) {
        if (!shareSongId.isNullOrBlank()) {
            viewModel.handleEvent(ChatContract.Event.SongShareClicked(shareSongId))
        }
    }

    DisposableEffect(Unit) {
        viewModel.handleEvent(ChatContract.Event.ScreenVisible)
        onDispose { viewModel.handleEvent(ChatContract.Event.ScreenHidden) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ChatContract.Effect.ScrollToBottom -> {
                    val total = pagingMessages.itemCount + state.tailMessages.size
                    if (total > 0) listState.animateScrollToItem(total - 1)
                }
                is ChatContract.Effect.PlaySong -> onPlaySong(effect.songId)
            }
        }
    }

    val mergedKeys = remember(pagingMessages.itemSnapshotList.items, state.tailMessages) {
        buildMergedMessages(
            pagingItems = pagingMessages.itemSnapshotList.items,
            tailMessages = state.tailMessages,
        )
    }

    LaunchedEffect(mergedKeys.size, pagingMessages.loadState.refresh) {
        if (mergedKeys.isNotEmpty() && pagingMessages.loadState.refresh is LoadState.NotLoading) {
            listState.scrollToItem(mergedKeys.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = state.otherUser?.displayName ?: stringResource(R.string.chat_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.isOtherTyping) {
                        Text(
                            text = stringResource(R.string.chat_typing),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
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

        when {
            state.isLoading -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                val error = state.error
                ErrorState(
                    message = error?.displayMessage(context) ?: "",
                    modifier = Modifier.weight(1f),
                    onRetry = {
                        viewModel.handleEvent(ChatContract.Event.Load(otherUserId, conversationId))
                    },
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(
                        horizontal = spacing.md,
                        vertical = spacing.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    if (pagingMessages.loadState.prepend is LoadState.Loading) {
                        item(key = "loading_older") {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    items(
                        count = mergedKeys.size,
                        key = { index -> mergedKeys[index].stableKey },
                    ) { index ->
                        val message = mergedKeys[index]
                        LaunchedEffect(message.stableKey) {
                            viewModel.handleEvent(ChatContract.Event.MessageVisible(message))
                        }
                        MessageBubble(
                            message = message,
                            status = state.statusOverrides[message.serverId ?: -1] ?: message.status,
                            onSongClick = onPlaySong,
                        )
                    }
                }

                ChatInputBar(
                    text = state.inputText,
                    isSending = state.isSending,
                    onTextChange = { viewModel.handleEvent(ChatContract.Event.InputChanged(it)) },
                    onSend = { viewModel.handleEvent(ChatContract.Event.SendClicked) },
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.chat_input_hint)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isSending,
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    status: MessageStatus,
    onSongClick: (String) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val bubbleColor = if (message.isMine) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val shape = RoundedCornerShape(
        topStart = spacing.md,
        topEnd = spacing.md,
        bottomStart = if (message.isMine) spacing.md else spacing.xs,
        bottomEnd = if (message.isMine) spacing.xs else spacing.md,
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(spacing.sm),
        ) {
            when (message.msgType) {
                MessageType.SONG -> SongShareCard(
                    message = message,
                    onClick = { message.songId?.let(onSongClick) },
                )
                else -> Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (message.isMine) {
                    MessageReceiptIcon(status = status)
                }
            }
        }
    }
}

@Composable
private fun SongShareCard(
    message: ChatMessage,
    onClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.sm))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeloImage(
            imageUrl = message.songCoverUrl,
            contentDescription = message.songTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(spacing.xs)),
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.songTitle ?: stringResource(R.string.chat_song_preview),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = message.songArtist.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.chat_tap_to_play),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = spacing.xs),
            )
        }
    }
}

@Composable
private fun MessageReceiptIcon(status: MessageStatus) {
    val tint = when (status) {
        MessageStatus.READ -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    when (status) {
        MessageStatus.PENDING, MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.chat_status_sent),
                modifier = Modifier.size(14.dp),
                tint = tint,
            )
        }
        MessageStatus.DELIVERED, MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = when (status) {
                    MessageStatus.READ -> stringResource(R.string.chat_status_read)
                    else -> stringResource(R.string.chat_status_delivered)
                },
                modifier = Modifier.size(14.dp),
                tint = tint,
            )
        }
    }
}

private fun buildMergedMessages(
    pagingItems: List<ChatMessage>,
    tailMessages: List<ChatMessage>,
): List<ChatMessage> {
    val merged = LinkedHashMap<String, ChatMessage>()
    pagingItems.forEach { merged[it.stableKey] = it }
    tailMessages.forEach { merged[it.stableKey] = it }
    return merged.values.sortedBy { it.createdAt }
}

private fun formatMessageTime(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
