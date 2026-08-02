package com.melonet.app.feature.chat

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.ChatPeer
import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.repository.ChatRepository
import com.melonet.app.data.repository.SocialRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val socialRepository: SocialRepository,
) : BaseViewModel<ChatContract.State, ChatContract.Event, ChatContract.Effect>() {

    private val conversationIdFlow = MutableStateFlow(0)
    private var receiverId: Int = 0
    private var typingJob: Job? = null
    private var isTyping = false
    private val readMessageIds = mutableSetOf<Long>()

    val messages: Flow<PagingData<ChatMessage>> = conversationIdFlow
        .flatMapLatest { id ->
            if (id == 0) flowOf(PagingData.empty()) else chatRepository.messages(id)
        }
        .cachedIn(viewModelScope)

    override fun createInitialState() = ChatContract.State()

    init {
        viewModelScope.launch {
            chatRepository.connectionState.collect { state ->
                setState { copy(connectionState = state) }
            }
        }
        viewModelScope.launch {
            chatRepository.realtimeMessages.collect { message ->
                if (message.conversationId != conversationIdFlow.value) return@collect
                setState {
                    val exists = tailMessages.any { it.stableKey == message.stableKey || it.localId == message.localId }
                    copy(
                        tailMessages = if (exists) {
                            tailMessages.map {
                                if (it.stableKey == message.stableKey || it.localId == message.localId) message else it
                            }
                        } else {
                            tailMessages + message
                        },
                    )
                }
                setEffect { ChatContract.Effect.ScrollToBottom(force = false) }
            }
        }
        viewModelScope.launch {
            chatRepository.messageUpdates.collect {
                syncStatusOverrides()
                refreshPendingTail()
            }
        }
        viewModelScope.launch {
            chatRepository.typingUsers.collect { typingMap ->
                val conversationId = conversationIdFlow.value
                setState {
                    copy(isOtherTyping = typingMap.containsKey(conversationId))
                }
            }
        }
    }

    override fun handleEvent(event: ChatContract.Event) {
        when (event) {
            is ChatContract.Event.Load -> load(event.otherUserId, event.conversationId)
            is ChatContract.Event.InputChanged -> onInputChanged(event.value)
            ChatContract.Event.SendClicked -> sendMessage()
            is ChatContract.Event.SongShareClicked -> shareSong(event.songId)
            is ChatContract.Event.MessageVisible -> markReadIfNeeded(event.message)
            is ChatContract.Event.RetryMessage -> retryMessage(event.localId)
            is ChatContract.Event.CancelMessage -> cancelMessage(event.localId)
            is ChatContract.Event.CopyMessage -> {
                val body = ChatReplyCodec.displayBody(event.text)
                if (body.isNotBlank()) {
                    setEffect { ChatContract.Effect.CopyToClipboard(body) }
                }
            }
            is ChatContract.Event.ReplyToMessage -> setReplyDraft(event.message, event.authorLabel)
            ChatContract.Event.CancelReply -> setState { copy(replyDraft = null) }
            ChatContract.Event.ScreenVisible -> Unit
            ChatContract.Event.ScreenHidden -> stopTyping()
        }
    }

    private fun setReplyDraft(message: ChatMessage, authorLabel: String) {
        val parsed = ChatReplyCodec.parse(message.content)
        val preview = when (message.msgType) {
            com.melonet.app.data.model.MessageType.SONG ->
                message.songTitle?.ifBlank { null } ?: "Song"
            else -> parsed.body.ifBlank { message.content }.take(80)
        }
        setState {
            copy(
                replyDraft = ChatContract.ReplyDraft(
                    messageId = message.serverId?.toString() ?: message.localId,
                    author = authorLabel,
                    preview = preview,
                ),
            )
        }
    }

    private fun load(otherUserId: Int, conversationId: Int) {
        receiverId = otherUserId
        viewModelScope.launch {
            // Do not reset shareHandled — Load can re-run and would re-send the song share.
            setState { copy(isLoading = true, error = null) }
            val cachedPeer = chatRepository.getCachedPeer(otherUserId)
            if (cachedPeer != null) {
                setState { copy(otherUser = cachedPeer) }
            }
            val resolvedConversation = if (conversationId > 0) {
                conversationId
            } else {
                when (val result = chatRepository.getOrCreateConversation(otherUserId)) {
                    is Result.Success -> {
                        setState { copy(otherUser = result.data.otherUser) }
                        result.data.id
                    }
                    is Result.Error -> {
                        setState { copy(isLoading = false, error = result.error) }
                        return@launch
                    }
                }
            }
            val previousConversation = uiState.value.conversationId
            conversationIdFlow.value = resolvedConversation
            if (uiState.value.otherUser == null) {
                when (val profile = socialRepository.getUserProfile(otherUserId)) {
                    is Result.Success -> {
                        val peer = ChatPeer(
                            id = profile.data.id,
                            username = profile.data.username,
                            displayName = profile.data.displayName,
                            avatarUrl = profile.data.avatarUrl,
                            isPremium = profile.data.isPremium,
                        )
                        chatRepository.cachePeer(peer)
                        setState { copy(otherUser = peer) }
                    }
                    is Result.Error -> {
                        // Keep provisional peer so header is never blank offline.
                        if (uiState.value.otherUser == null) {
                            setState {
                                copy(
                                    otherUser = ChatPeer(
                                        id = otherUserId,
                                        username = "",
                                        displayName = "User $otherUserId",
                                        avatarUrl = null,
                                        isPremium = false,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            val cached = chatRepository.getCachedMessages(resolvedConversation)
            setState {
                copy(
                    isLoading = false,
                    conversationId = resolvedConversation,
                    tailMessages = mergeTail(
                        if (previousConversation == resolvedConversation) tailMessages else emptyList(),
                        cached.filter { isOutboundTail(it) },
                    ),
                )
            }
            syncStatusOverrides()
        }
    }

    private fun onInputChanged(value: String) {
        setState { copy(inputText = value) }
        val conversationId = uiState.value.conversationId
        if (conversationId == 0) return
        typingJob?.cancel()
        if (value.isNotBlank()) {
            if (!isTyping) {
                isTyping = true
                chatRepository.sendTypingStart(conversationId)
            }
            typingJob = viewModelScope.launch {
                delay(TYPING_IDLE_MS)
                stopTyping()
            }
        } else {
            stopTyping()
        }
    }

    private fun stopTyping() {
        val conversationId = uiState.value.conversationId
        if (isTyping && conversationId > 0) {
            chatRepository.sendTypingStop(conversationId)
        }
        isTyping = false
        typingJob?.cancel()
    }

    private fun sendMessage() {
        val text = uiState.value.inputText.trim()
        val conversationId = uiState.value.conversationId
        if (text.isBlank() || conversationId == 0 || receiverId == 0) return
        val reply = uiState.value.replyDraft
        val payload = if (reply != null) {
            ChatReplyCodec.encode(
                replyToId = reply.messageId,
                author = reply.author,
                preview = reply.preview,
                body = text,
            )
        } else {
            text
        }
        viewModelScope.launch {
            setState { copy(isSending = true, inputText = "", replyDraft = null) }
            stopTyping()
            val pending = chatRepository.sendTextMessage(conversationId, receiverId, payload)
            setState {
                copy(
                    isSending = false,
                    tailMessages = upsertTail(tailMessages, pending),
                )
            }
            setEffect { ChatContract.Effect.ScrollToBottom(force = true) }
        }
    }

    private fun shareSong(songId: String) {
        if (uiState.value.shareHandled) return
        val conversationId = uiState.value.conversationId
        if (conversationId == 0 || receiverId == 0) return
        if (!chatRepository.tryConsumeSongShare(conversationId, songId)) {
            setState { copy(shareHandled = true) }
            return
        }
        viewModelScope.launch {
            setState { copy(shareHandled = true) }
            val pending = chatRepository.sendSongShare(conversationId, receiverId, songId)
            setState { copy(tailMessages = upsertTail(tailMessages, pending)) }
            setEffect { ChatContract.Effect.ScrollToBottom(force = true) }
        }
    }

    private fun retryMessage(localId: String) {
        viewModelScope.launch {
            val updated = chatRepository.retryMessage(localId) ?: return@launch
            setState { copy(tailMessages = upsertTail(tailMessages, updated)) }
        }
    }

    private fun cancelMessage(localId: String) {
        viewModelScope.launch {
            chatRepository.cancelMessage(localId)
            setState {
                copy(tailMessages = tailMessages.filterNot { it.localId == localId })
            }
        }
    }

    private fun markReadIfNeeded(message: ChatMessage) {
        if (message.isMine || message.serverId == null) return
        if (message.status == MessageStatus.READ) return
        val id = message.serverId
        if (!readMessageIds.add(id)) return
        val conversationId = uiState.value.conversationId
        viewModelScope.launch {
            chatRepository.markConversationRead(conversationId, listOf(id))
            setState {
                copy(statusOverrides = statusOverrides + (id to MessageStatus.READ))
            }
        }
    }

    /**
     * Refresh tail from cache without wiping inbound realtime or ACKed outbound
     * messages that are not yet present in the paging snapshot.
     */
    private suspend fun refreshPendingTail() {
        val conversationId = uiState.value.conversationId
        if (conversationId == 0) return
        val cached = chatRepository.getCachedMessages(conversationId)
        setState {
            copy(tailMessages = mergeTail(tailMessages, cached))
        }
    }

    private suspend fun syncStatusOverrides() {
        val conversationId = uiState.value.conversationId
        if (conversationId == 0) return
        val cached = chatRepository.getCachedMessages(conversationId)
        val overrides = cached.mapNotNull { message ->
            message.serverId?.let { id -> id to message.status }
        }.toMap()
        setState { copy(statusOverrides = overrides) }
    }

    private fun isOutboundTail(message: ChatMessage): Boolean =
        message.serverId == null ||
            message.status == MessageStatus.PENDING ||
            message.status == MessageStatus.FAILED

    private fun upsertTail(current: List<ChatMessage>, message: ChatMessage): List<ChatMessage> {
        val index = current.indexOfFirst { it.localId == message.localId || it.stableKey == message.stableKey }
        return if (index >= 0) {
            current.toMutableList().also { it[index] = message }
        } else {
            current + message
        }
    }

    private fun mergeTail(current: List<ChatMessage>, cached: List<ChatMessage>): List<ChatMessage> {
        val byLocal = cached.associateBy { it.localId }
        val byServer = cached.mapNotNull { msg -> msg.serverId?.let { it to msg } }.toMap()
        val merged = current.map { msg ->
            byLocal[msg.localId]
                ?: msg.serverId?.let { byServer[it] }
                ?: msg
        }.toMutableList()
        for (msg in cached) {
            if (!isOutboundTail(msg)) continue
            val index = merged.indexOfFirst { it.localId == msg.localId || it.stableKey == msg.stableKey }
            if (index >= 0) merged[index] = msg else merged += msg
        }
        return merged.distinctBy { it.stableKey }
    }

    private companion object {
        const val TYPING_IDLE_MS = 2_000L
    }
}
