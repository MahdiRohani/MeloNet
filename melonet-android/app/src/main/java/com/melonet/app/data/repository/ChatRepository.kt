package com.melonet.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.NetworkConnectivityMonitor
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.local.ChatMessageDao
import com.melonet.app.data.mapper.ChatMapper
import com.melonet.app.data.model.ChatConnectionState
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.ChatPeer
import com.melonet.app.data.model.Conversation
import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.model.MessageType
import com.melonet.app.data.paging.ConversationsPagingSource
import com.melonet.app.data.paging.MessagesPagingSource
import com.melonet.app.data.realtime.ChatWebSocketClient
import com.melonet.app.data.realtime.ChatWsEvent
import com.melonet.app.data.realtime.WsMessageReadPayload
import com.melonet.app.data.realtime.WsMessageSendPayload
import com.melonet.app.data.realtime.WsSocketState
import com.melonet.app.data.remote.ChatApi
import com.melonet.app.data.remote.dto.CreateConversationRequestDto
import com.melonet.app.data.remote.dto.MarkReadRequestDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatRepository(
    private val chatApi: ChatApi,
    private val chatMessageDao: ChatMessageDao,
    private val webSocketClient: ChatWebSocketClient,
    private val playerRepository: PlayerRepository,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val dispatchers: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private var currentUserId: Int = 0

    private val peerCache = ConcurrentHashMap<Int, ChatPeer>()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _typingUsers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val typingUsers: StateFlow<Map<Int, Int>> = _typingUsers.asStateFlow()

    private val _realtimeMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val realtimeMessages: SharedFlow<ChatMessage> = _realtimeMessages.asSharedFlow()

    private val _messageUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val messageUpdates: SharedFlow<Unit> = _messageUpdates.asSharedFlow()

    private val _conversationUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val conversationUpdates: SharedFlow<Unit> = _conversationUpdates.asSharedFlow()

    private val _isOnline = MutableStateFlow(true)
    val connectionState: StateFlow<ChatConnectionState> = combine(
        webSocketClient.socketState,
        _isOnline,
    ) { socket, online ->
        when {
            !online -> ChatConnectionState.Offline
            socket == WsSocketState.Connected -> ChatConnectionState.Connected
            socket == WsSocketState.Reconnecting -> ChatConnectionState.Reconnecting
            else -> ChatConnectionState.Offline
        }
    }.distinctUntilChanged()
        .stateInScope(ChatConnectionState.Offline)

    init {
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
            }
        }
        scope.launch {
            webSocketClient.events.collect { event -> handleWsEvent(event) }
        }
    }

    fun setCurrentUserId(userId: Int) {
        currentUserId = userId
    }

    fun connect() {
        webSocketClient.connect()
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    fun cachePeer(peer: ChatPeer) {
        if (peer.id > 0) peerCache[peer.id] = peer
    }

    fun getCachedPeer(userId: Int): ChatPeer? = peerCache[userId]

    fun conversations(): Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            ConversationsPagingSource(chatApi) { items ->
                items.forEach { cachePeer(it.otherUser) }
            }
        },
    ).flow

    fun messages(conversationId: Int): Flow<PagingData<ChatMessage>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false),
        pagingSourceFactory = {
            MessagesPagingSource(
                chatApi = chatApi,
                chatMessageDao = chatMessageDao,
                conversationId = conversationId,
                currentUserId = currentUserId,
            )
        },
    ).flow

    suspend fun refreshUnreadCount(): Result<Int> = withContext(dispatchers.io) {
        when (val result = safeApiCall { chatApi.unreadCount() }) {
            is Result.Success -> {
                _unreadCount.value = result.data.total
                Result.Success(result.data.total)
            }
            is Result.Error -> result
        }
    }

    suspend fun getOrCreateConversation(otherUserId: Int): Result<Conversation> = withContext(dispatchers.io) {
        when (val result = safeApiCall { chatApi.createConversation(CreateConversationRequestDto(otherUserId)) }) {
            is Result.Success -> {
                val conversation = ChatMapper.toConversation(result.data)
                cachePeer(conversation.otherUser)
                Result.Success(conversation)
            }
            is Result.Error -> result
        }
    }

    suspend fun getCachedMessages(conversationId: Int): List<ChatMessage> = withContext(dispatchers.io) {
        chatMessageDao.getByConversation(conversationId)
            .map { ChatMapper.fromEntity(it, currentUserId) }
    }

    suspend fun sendTextMessage(
        conversationId: Int,
        receiverId: Int,
        content: String,
    ): ChatMessage = withContext(dispatchers.io) {
        val clientId = UUID.randomUUID().toString()
        val pending = ChatMessage(
            localId = clientId,
            serverId = null,
            conversationId = conversationId,
            senderId = currentUserId,
            receiverId = receiverId,
            content = content.trim(),
            msgType = MessageType.TEXT,
            songId = null,
            status = MessageStatus.PENDING,
            createdAt = Instant.now(),
            isMine = true,
        )
        chatMessageDao.upsert(ChatMapper.toEntity(pending))
        val sent = webSocketClient.sendText(
            WsMessageSendPayload(
                conversation_id = conversationId,
                receiver_id = receiverId,
                content = pending.content,
                msg_type = MessageType.toApi(MessageType.TEXT),
                client_id = clientId,
            ),
        )
        finalizeOutbound(pending, sent)
    }

    suspend fun sendSongShare(
        conversationId: Int,
        receiverId: Int,
        songId: String,
    ): ChatMessage = withContext(dispatchers.io) {
        val clientId = UUID.randomUUID().toString()
        val songIdLong = songId.toLongOrNull()
        val pending = ChatMessage(
            localId = clientId,
            serverId = null,
            conversationId = conversationId,
            senderId = currentUserId,
            receiverId = receiverId,
            content = "",
            msgType = MessageType.SONG,
            songId = songId,
            status = MessageStatus.PENDING,
            createdAt = Instant.now(),
            isMine = true,
        )
        val enriched = enrichSongMessage(pending)
        chatMessageDao.upsert(ChatMapper.toEntity(enriched))
        val sent = webSocketClient.sendSongShare(
            WsMessageSendPayload(
                conversation_id = conversationId,
                receiver_id = receiverId,
                msg_type = MessageType.toApi(MessageType.SONG),
                song_id = songIdLong,
                client_id = clientId,
            ),
        )
        finalizeOutbound(enriched, sent)
    }

    suspend fun retryMessage(localId: String): ChatMessage? = withContext(dispatchers.io) {
        val entity = chatMessageDao.getByLocalId(localId) ?: return@withContext null
        val message = ChatMapper.fromEntity(entity, currentUserId)
        if (!message.isMine) return@withContext null
        if (message.status != MessageStatus.FAILED && message.status != MessageStatus.PENDING) {
            return@withContext message
        }
        chatMessageDao.updateStatusByLocalId(localId, MessageStatus.PENDING.name)
        val pending = message.copy(status = MessageStatus.PENDING)
        val sent = when (pending.msgType) {
            MessageType.SONG -> webSocketClient.sendSongShare(
                WsMessageSendPayload(
                    conversation_id = pending.conversationId,
                    receiver_id = pending.receiverId,
                    msg_type = MessageType.toApi(MessageType.SONG),
                    song_id = pending.songId?.toLongOrNull(),
                    client_id = pending.localId,
                ),
            )
            else -> webSocketClient.sendText(
                WsMessageSendPayload(
                    conversation_id = pending.conversationId,
                    receiver_id = pending.receiverId,
                    content = pending.content,
                    msg_type = MessageType.toApi(MessageType.TEXT),
                    client_id = pending.localId,
                ),
            )
        }
        finalizeOutbound(pending, sent)
    }

    suspend fun cancelMessage(localId: String) = withContext(dispatchers.io) {
        val entity = chatMessageDao.getByLocalId(localId) ?: return@withContext
        if (entity.serverId != null) return@withContext
        chatMessageDao.deleteByLocalId(localId)
        _messageUpdates.emit(Unit)
        _conversationUpdates.emit(Unit)
    }

    suspend fun flushOutbox() = withContext(dispatchers.io) {
        val pending = chatMessageDao.getByStatuses(listOf(MessageStatus.PENDING.name, MessageStatus.FAILED.name))
        for (entity in pending) {
            if (entity.serverId != null) continue
            retryMessage(entity.localId)
        }
    }

    suspend fun markConversationRead(conversationId: Int, messageIds: List<Long>) = withContext(dispatchers.io) {
        if (messageIds.isEmpty()) return@withContext
        safeApiCall {
            chatApi.markRead(conversationId, MarkReadRequestDto(messageIds))
        }
        webSocketClient.sendRead(
            WsMessageReadPayload(conversation_id = conversationId, message_ids = messageIds),
        )
        chatMessageDao.updateStatus(conversationId, messageIds, MessageStatus.READ.name)
        _messageUpdates.emit(Unit)
        _conversationUpdates.emit(Unit)
        refreshUnreadCount()
    }

    fun sendTypingStart(conversationId: Int) {
        webSocketClient.sendTypingStart(conversationId)
    }

    fun sendTypingStop(conversationId: Int) {
        webSocketClient.sendTypingStop(conversationId)
    }

    suspend fun enrichSongMessage(message: ChatMessage): ChatMessage {
        val songId = message.songId ?: return message
        if (message.songTitle != null) return message
        return when (val result = playerRepository.getSong(songId)) {
            is Result.Success -> message.copy(
                songTitle = result.data.title,
                songArtist = result.data.artistName,
                songCoverUrl = result.data.coverUrl,
            )
            is Result.Error -> message
        }
    }

    private suspend fun finalizeOutbound(message: ChatMessage, sent: Boolean): ChatMessage {
        val result = if (sent) {
            message
        } else {
            val failed = message.copy(status = MessageStatus.FAILED)
            chatMessageDao.updateStatusByLocalId(message.localId, MessageStatus.FAILED.name)
            failed
        }
        _messageUpdates.emit(Unit)
        _conversationUpdates.emit(Unit)
        return result
    }

    private suspend fun handleWsEvent(event: ChatWsEvent) {
        when (event) {
            ChatWsEvent.Connected -> {
                refreshUnreadCount()
                flushOutbox()
                _conversationUpdates.emit(Unit)
            }
            is ChatWsEvent.MessageAck -> handleAck(event)
            is ChatWsEvent.MessageNew -> handleNewMessage(event)
            is ChatWsEvent.MessageDelivered -> handleDelivered(event)
            is ChatWsEvent.MessageRead -> handleRead(event)
            is ChatWsEvent.Typing -> handleTyping(event)
            is ChatWsEvent.Error, ChatWsEvent.Disconnected -> Unit
        }
    }

    private suspend fun handleAck(event: ChatWsEvent.MessageAck) {
        val clientId = event.clientId ?: return
        val existing = chatMessageDao.getByLocalId(clientId) ?: return
        val ack = event.message
        // Keep client UUID as localId so UI keys stay stable across ack.
        val updated = existing.copy(
            serverId = ack.id,
            status = MessageStatus.fromApi(ack.status).name,
            conversationId = ack.conversationId.takeIf { it > 0 } ?: existing.conversationId,
        )
        chatMessageDao.upsert(updated)
        // Drop any paging duplicate keyed as server_{id}.
        chatMessageDao.deleteByLocalId("server_${ack.id}")
        _messageUpdates.emit(Unit)
        _conversationUpdates.emit(Unit)
    }

    private suspend fun handleNewMessage(event: ChatWsEvent.MessageNew) {
        var message = ChatMapper.toMessage(event.message, currentUserId)
        if (message.msgType == MessageType.SONG) {
            message = enrichSongMessage(message)
        }
        val existing = message.serverId?.let { chatMessageDao.getByServerId(it) }
        val toStore = if (existing != null) {
            message.copy(
                localId = existing.localId,
                songTitle = message.songTitle ?: existing.songTitle,
                songArtist = message.songArtist ?: existing.songArtist,
                songCoverUrl = message.songCoverUrl ?: existing.songCoverUrl,
            )
        } else {
            message
        }
        chatMessageDao.upsert(ChatMapper.toEntity(toStore))
        _realtimeMessages.emit(toStore)
        _messageUpdates.emit(Unit)
        _conversationUpdates.emit(Unit)
        if (!toStore.isMine) {
            refreshUnreadCount()
        }
    }

    private suspend fun handleDelivered(event: ChatWsEvent.MessageDelivered) {
        val entity = chatMessageDao.getByServerId(event.messageId) ?: return
        if (entity.status == MessageStatus.READ.name) return
        chatMessageDao.upsert(entity.copy(status = MessageStatus.DELIVERED.name))
        _messageUpdates.emit(Unit)
    }

    private suspend fun handleRead(event: ChatWsEvent.MessageRead) {
        if (event.messageIds.isNotEmpty()) {
            chatMessageDao.updateStatus(
                event.conversationId,
                event.messageIds,
                MessageStatus.READ.name,
            )
        } else {
            chatMessageDao.updateAllFromSender(
                conversationId = event.conversationId,
                senderId = currentUserId,
                status = MessageStatus.READ.name,
            )
        }
        _messageUpdates.emit(Unit)
        _conversationUpdates.emit(Unit)
    }

    private fun handleTyping(event: ChatWsEvent.Typing) {
        if (event.userId == currentUserId) return
        _typingUsers.value = if (event.isTyping) {
            _typingUsers.value + (event.conversationId to event.userId)
        } else {
            _typingUsers.value - event.conversationId
        }
    }

    private fun <T> Flow<T>.stateInScope(initial: T): StateFlow<T> {
        val state = MutableStateFlow(initial)
        scope.launch { collect { state.value = it } }
        return state.asStateFlow()
    }
}
