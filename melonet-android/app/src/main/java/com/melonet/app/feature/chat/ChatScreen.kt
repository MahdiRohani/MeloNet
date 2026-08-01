package com.melonet.app.feature.chat

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.melonet.app.R
import com.melonet.app.core.common.displayMessage
import com.melonet.app.core.designsystem.component.ChatConnectionBanner
import com.melonet.app.core.designsystem.component.ErrorState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloMotion
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.model.MessageType
import com.melonet.app.data.repository.ChatRepository
import com.melonet.app.feature.player.PlaybackAudioBridge
import com.melonet.app.feature.player.PlaybackManager
import com.melonet.app.feature.player.component.AudioVisualizer
import org.koin.compose.koinInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private sealed interface ChatListItem {
    data class DateHeader(val labelKey: String, val epochDay: Long) : ChatListItem
    data class Bubble(val message: ChatMessage) : ChatListItem
    data object Typing : ChatListItem
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val chatRepository: ChatRepository = koinInject()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(otherUserId, conversationId) {
        viewModel.handleEvent(ChatContract.Event.Load(otherUserId, conversationId))
    }

    LaunchedEffect(shareSongId, state.conversationId, state.shareHandled) {
        if (!shareSongId.isNullOrBlank() && state.conversationId > 0 && !state.shareHandled) {
            viewModel.handleEvent(ChatContract.Event.SongShareClicked(shareSongId))
        }
    }

    DisposableEffect(Unit) {
        viewModel.handleEvent(ChatContract.Event.ScreenVisible)
        onDispose { viewModel.handleEvent(ChatContract.Event.ScreenHidden) }
    }

    val mergedMessages = remember(pagingMessages.itemSnapshotList.items, state.tailMessages, state.statusOverrides) {
        buildMergedMessages(
            pagingItems = pagingMessages.itemSnapshotList.items,
            tailMessages = state.tailMessages,
            statusOverrides = state.statusOverrides,
        )
    }
    val listItems = remember(mergedMessages, state.isOtherTyping) {
        buildChatListItems(mergedMessages, state.isOtherTyping)
    }

    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || lastVisible >= total - 3
        }
    }
    val nearBottomState = rememberUpdatedState(isNearBottom)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatContract.Effect.ScrollToBottom -> {
                    if (listItems.isNotEmpty() && (effect.force || nearBottomState.value)) {
                        listState.animateScrollToItem(listItems.lastIndex)
                    }
                }
                is ChatContract.Effect.PlaySong -> onPlaySong(effect.songId)
                is ChatContract.Effect.CopyToClipboard -> {
                    clipboard.setText(AnnotatedString(effect.text))
                    Toast.makeText(context, context.getString(R.string.chat_copy_message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(state.conversationId, pagingMessages.loadState.refresh) {
        if (listItems.isNotEmpty() && pagingMessages.loadState.refresh is LoadState.NotLoading) {
            listState.scrollToItem(listItems.lastIndex)
        }
    }

    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottomPx) {
        if (imeBottomPx > 0 && listItems.isNotEmpty()) {
            listState.animateScrollToItem(listItems.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeloImage(
                        imageUrl = state.otherUser?.avatarUrl,
                        contentDescription = state.otherUser?.displayName,
                        contentScale = ContentScale.Crop,
                        targetSize = 42.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Column {
                        Text(
                            text = state.otherUser?.displayName ?: stringResource(R.string.chat_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        when {
                            state.isOtherTyping -> TypingLabel()
                            !state.otherUser?.username.isNullOrBlank() -> {
                                Text(
                                    text = "@${state.otherUser?.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            else -> {
                                Text(
                                    text = stringResource(R.string.chat_online),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background),
        )

        ChatConnectionBanner(
            state = state.connectionState,
            onRetryConnect = { chatRepository.connect() },
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
                        count = listItems.size,
                        key = { index ->
                            when (val item = listItems[index]) {
                                is ChatListItem.DateHeader -> "date_${item.epochDay}"
                                is ChatListItem.Bubble -> item.message.stableKey
                                ChatListItem.Typing -> "typing"
                            }
                        },
                    ) { index ->
                        when (val item = listItems[index]) {
                            is ChatListItem.DateHeader -> DateSeparator(item.labelKey)
                            is ChatListItem.Bubble -> {
                                LaunchedEffect(item.message.stableKey) {
                                    viewModel.handleEvent(ChatContract.Event.MessageVisible(item.message))
                                }
                                MessageBubble(
                                    message = item.message,
                                    status = item.message.status,
                                    onOpenPlayer = onPlaySong,
                                    onRetry = {
                                        viewModel.handleEvent(ChatContract.Event.RetryMessage(item.message.localId))
                                    },
                                    onCancel = {
                                        viewModel.handleEvent(ChatContract.Event.CancelMessage(item.message.localId))
                                    },
                                    onLongPressCopy = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.handleEvent(ChatContract.Event.CopyMessage(item.message.content))
                                    },
                                )
                            }
                            ChatListItem.Typing -> TypingBubble()
                        }
                    }
                }

                ChatInputBar(
                    text = state.inputText,
                    isSending = state.isSending,
                    enabled = !state.isSending,
                    onTextChange = { viewModel.handleEvent(ChatContract.Event.InputChanged(it)) },
                    onSend = {
                        if (state.isSending) return@ChatInputBar
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.handleEvent(ChatContract.Event.SendClicked)
                    },
                )
            }
        }
    }
}

@Composable
private fun TypingLabel() {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typing_phase",
    )
    val dots = ".".repeat((phase.toInt() % 3) + 1)
    Text(
        text = stringResource(R.string.chat_typing).removeSuffix("…") + dots,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun TypingBubble() {
    val spacing = MeloNetTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                val transition = rememberInfiniteTransition(label = "dot_$index")
                val alpha by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, delayMillis = index * 120),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dot_alpha_$index",
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { this.alpha = alpha }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun DateSeparator(labelKey: String) {
    val label = when (labelKey) {
        "today" -> stringResource(R.string.chat_today)
        "yesterday" -> stringResource(R.string.chat_yesterday)
        else -> labelKey
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isSending: Boolean,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val canSend = text.isNotBlank() && enabled && !isSending

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = spacing.sm, vertical = spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.surfaceVariant.copy(alpha = 0.85f))
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.chat_input_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) scheme.primary else scheme.outline.copy(alpha = 0.35f))
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = scheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send),
                        tint = if (canSend) scheme.onPrimary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    status: MessageStatus,
    onOpenPlayer: (String) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onLongPressCopy: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    var appeared by remember(message.stableKey) { mutableStateOf(false) }
    LaunchedEffect(message.stableKey) { appeared = true }
    val enterAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = MeloMotion.fadeTween,
        label = "bubble_alpha",
    )
    val enterOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 10f,
        animationSpec = MeloMotion.pressSpring,
        label = "bubble_offset",
    )

    val bubbleColor = when {
        status == MessageStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        message.isMine -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        status == MessageStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        message.isMine -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val metaColor = when {
        status == MessageStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
        message.isMine -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (message.isMine) 18.dp else 5.dp,
        bottomEnd = if (message.isMine) 5.dp else 18.dp,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enterAlpha
                translationY = enterOffset
            },
        contentAlignment = alignment,
    ) {
        Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(shape)
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (message.msgType == MessageType.TEXT && message.content.isNotBlank()) {
                                onLongPressCopy()
                            }
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                when (message.msgType) {
                    MessageType.SONG -> SongShareCard(
                        message = message,
                        isMine = message.isMine && status != MessageStatus.FAILED,
                        onOpenPlayer = onOpenPlayer,
                    )
                    else -> Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    Text(
                        text = formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = metaColor,
                    )
                    if (message.isMine) {
                        MessageReceiptIcon(status = status, tint = metaColor)
                    }
                }
            }
            if (status == MessageStatus.FAILED && message.isMine) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.chat_retry_message))
                    }
                    TextButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.chat_cancel_message))
                    }
                }
            }
        }
    }
}

@Composable
private fun SongShareCard(
    message: ChatMessage,
    isMine: Boolean,
    onOpenPlayer: (String) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val playbackManager: PlaybackManager = koinInject()
    val playback by playbackManager.state.collectAsState()
    val fftMagnitudes by PlaybackAudioBridge.fftMagnitudes.collectAsState()

    val songId = message.songId
    val hasMeta = !message.songTitle.isNullOrBlank()
    val loadingMeta = songId != null && !hasMeta
    val isThisSong = songId != null && playback.currentSong?.id == songId
    val isPlayingThis = isThisSong && playback.isPlaying
    val isBuffering = isThisSong && playback.isLoading
    val progress = if (isThisSong && playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    val bg = if (isMine) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    }
    val titleColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val accent = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    fun togglePlayback() {
        val id = songId ?: return
        when {
            isThisSong -> playbackManager.togglePlayPause()
            else -> playbackManager.playSongId(id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MeloImage(
                imageUrl = message.songCoverUrl,
                contentDescription = message.songTitle,
                contentScale = ContentScale.Crop,
                targetSize = 56.dp,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = songId != null) {
                        songId?.let(onOpenPlayer)
                    },
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        hasMeta -> message.songTitle.orEmpty()
                        loadingMeta -> stringResource(R.string.chat_song_loading)
                        else -> stringResource(R.string.chat_song_unavailable)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        isBuffering -> stringResource(R.string.chat_song_buffering)
                        hasMeta -> message.songArtist.orEmpty().ifBlank {
                            stringResource(R.string.chat_tap_to_play)
                        }
                        else -> stringResource(R.string.chat_song_preview)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (isMine) 0.22f else 0.16f))
                    .clickable(enabled = songId != null && !loadingMeta, onClick = ::togglePlayback),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isBuffering -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accent,
                    )
                    isPlayingThis -> Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = stringResource(R.string.chat_pause),
                        tint = accent,
                    )
                    else -> Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.chat_tap_to_play),
                        tint = accent,
                    )
                }
            }
        }

        if (isThisSong) {
            Spacer(modifier = Modifier.height(spacing.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent),
                )
            }
            if (isPlayingThis || isBuffering) {
                Spacer(modifier = Modifier.height(spacing.xs))
                AudioVisualizer(
                    isPlaying = isPlayingThis,
                    magnitudes = fftMagnitudes,
                    barCount = 28,
                    height = 28.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MessageReceiptIcon(status: MessageStatus, tint: androidx.compose.ui.graphics.Color) {
    when (status) {
        MessageStatus.FAILED -> {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = stringResource(R.string.chat_status_failed),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        MessageStatus.PENDING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = tint,
            )
        }
        MessageStatus.SENT -> {
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
                tint = if (status == MessageStatus.READ) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    tint
                },
            )
        }
    }
}

private fun buildMergedMessages(
    pagingItems: List<ChatMessage>,
    tailMessages: List<ChatMessage>,
    statusOverrides: Map<Long, MessageStatus>,
): List<ChatMessage> {
    val merged = LinkedHashMap<String, ChatMessage>()
    fun put(message: ChatMessage) {
        val withStatus = message.serverId?.let { id ->
            statusOverrides[id]?.let { message.copy(status = it) }
        } ?: message
        val existing = merged[withStatus.stableKey]
        merged[withStatus.stableKey] = when {
            existing == null -> withStatus
            existing.localId.contains('-') && !withStatus.localId.contains('-') ->
                existing.copy(
                    serverId = withStatus.serverId ?: existing.serverId,
                    status = withStatus.status,
                    songTitle = existing.songTitle ?: withStatus.songTitle,
                    songArtist = existing.songArtist ?: withStatus.songArtist,
                    songCoverUrl = existing.songCoverUrl ?: withStatus.songCoverUrl,
                )
            else -> withStatus.copy(
                localId = if (existing.localId.contains('-')) existing.localId else withStatus.localId,
                songTitle = withStatus.songTitle ?: existing.songTitle,
                songArtist = withStatus.songArtist ?: existing.songArtist,
                songCoverUrl = withStatus.songCoverUrl ?: existing.songCoverUrl,
            )
        }
    }
    pagingItems.forEach(::put)
    tailMessages.forEach(::put)
    return merged.values.sortedBy { it.createdAt }
}

private fun buildChatListItems(
    messages: List<ChatMessage>,
    isTyping: Boolean,
): List<ChatListItem> {
    if (messages.isEmpty() && !isTyping) return emptyList()
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val items = mutableListOf<ChatListItem>()
    var lastDay: LocalDate? = null
    for (message in messages) {
        val day = message.createdAt.atZone(zone).toLocalDate()
        if (day != lastDay) {
            val label = when (ChronoUnit.DAYS.between(day, today)) {
                0L -> "today"
                1L -> "yesterday"
                else -> day.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
            items += ChatListItem.DateHeader(label, day.toEpochDay())
            lastDay = day
        }
        items += ChatListItem.Bubble(message)
    }
    if (isTyping) items += ChatListItem.Typing
    return items
}

private fun formatMessageTime(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
