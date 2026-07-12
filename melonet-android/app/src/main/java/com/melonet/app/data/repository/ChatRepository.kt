package com.melonet.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.local.ChatMessageDao
import com.melonet.app.data.mapper.ChatMapper
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.Conversation
import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.model.MessageType
import com.melonet.app.data.paging.ConversationsPagingSource
import com.melonet.app.data.paging.MessagesPagingSource
import com.melonet.app.data.realtime.ChatWebSocketClient
import com.melonet.app.data.realtime.ChatWsEvent
import com.melonet.app.data.realtime.WsMessageReadPayload
import com.melonet.app.data.realtime.WsMessageSendPayload
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class ChatRepository(
    private val chatApi: ChatApi,
    private val chatMessageDao: ChatMessageDao,
    private val webSocketClient: ChatWebSocketClient,
    private val playerRepository: PlayerRepository,
    private val dispatchers: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private var currentUserId: Int = 0

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _typingUsers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val typingUsers: StateFlow<Map<Int, Int>> = _typingUsers.asStateFlow()

    private val _realtimeMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val realtimeMessages: SharedFlow<ChatMessage> = _realtimeMessages.asSharedFlow()

    private val _messageUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val messageUpdates: SharedFlow<Unit> = _messageUpdates.asSharedFlow()

    init {
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

    fun conversations(): Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { ConversationsPagingSource(chatApi) },
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
            is Result.Success -> Result.Success(ChatMapper.toConversation(result.data))
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
        webSocketClient.sendText(
            WsMessageSendPayload(
                conversation_id = conversationId,
                receiver_id = receiverId,
                content = pending.content,
                msg_type = MessageType.toApi(MessageType.TEXT),
                client_id = clientId,
            ),
        )
        pending
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
        webSocketClient.sendSongShare(
            WsMessageSendPayload(
                conversation_id = conversationId,
                receiver_id = receiverId,
                msg_type = MessageType.toApi(MessageType.SONG),
                song_id = songIdLong,
                client_id = clientId,
            ),
        )
        enriched
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

    private suspend fun handleWsEvent(event: ChatWsEvent) {
        when (event) {
            ChatWsEvent.Connected -> refreshUnreadCount()
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
        val updated = existing.copy(
            localId = "server_${ack.id}",
            serverId = ack.id,
            status = MessageStatus.fromApi(ack.status).name,
            conversationId = ack.conversationId.takeIf { it > 0 } ?: existing.conversationId,
        )
        chatMessageDao.deleteByLocalId(clientId)
        chatMessageDao.upsert(updated)
        _messageUpdates.emit(Unit)
    }

    private suspend fun handleNewMessage(event: ChatWsEvent.MessageNew) {
        var message = ChatMapper.toMessage(event.message, currentUserId)
        if (message.msgType == MessageType.SONG) {
            message = enrichSongMessage(message)
        }
        chatMessageDao.upsert(ChatMapper.toEntity(message))
        _realtimeMessages.emit(message)
        _messageUpdates.emit(Unit)
        if (!message.isMine) {
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
    }

    private fun handleTyping(event: ChatWsEvent.Typing) {
        if (event.userId == currentUserId) return
        _typingUsers.value = if (event.isTyping) {
            _typingUsers.value + (event.conversationId to event.userId)
        } else {
            _typingUsers.value - event.conversationId
        }
    }
}
