package com.melonet.app.feature.chat

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.repository.ChatRepository
import com.melonet.app.data.repository.SocialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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
            chatRepository.realtimeMessages.collect { message ->
                if (message.conversationId != conversationIdFlow.value) return@collect
                setState {
                    val exists = tailMessages.any { it.stableKey == message.stableKey }
                    copy(
                        tailMessages = if (exists) tailMessages else tailMessages + message,
                    )
                }
                setEffect { ChatContract.Effect.ScrollToBottom }
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
            ChatContract.Event.ScreenVisible -> Unit
            ChatContract.Event.ScreenHidden -> stopTyping()
        }
    }

    private fun load(otherUserId: Int, conversationId: Int) {
        receiverId = otherUserId
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val resolvedConversation = if (conversationId > 0) {
                conversationId
            } else {
                when (val result = chatRepository.getOrCreateConversation(otherUserId)) {
                    is Result.Success -> {
                        setState {
                            copy(otherUser = result.data.otherUser)
                        }
                        result.data.id
                    }
                    is Result.Error -> {
                        setState { copy(isLoading = false, error = result.error) }
                        return@launch
                    }
                }
            }
            conversationIdFlow.value = resolvedConversation
            if (uiState.value.otherUser == null) {
                when (val profile = socialRepository.getUserProfile(otherUserId)) {
                    is Result.Success -> setState {
                        copy(
                            otherUser = com.melonet.app.data.model.ChatPeer(
                                id = profile.data.id,
                                username = profile.data.username,
                                displayName = profile.data.displayName,
                                avatarUrl = profile.data.avatarUrl,
                                isPremium = profile.data.isPremium,
                            ),
                        )
                    }
                    is Result.Error -> Unit
                }
            }
            val cached = chatRepository.getCachedMessages(resolvedConversation)
            setState {
                copy(
                    isLoading = false,
                    conversationId = resolvedConversation,
                    tailMessages = cached.filter { cachedMsg ->
                        cachedMsg.serverId == null || cachedMsg.status == MessageStatus.PENDING
                    },
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
        viewModelScope.launch {
            setState { copy(isSending = true, inputText = "") }
            stopTyping()
            val pending = chatRepository.sendTextMessage(conversationId, receiverId, text)
            setState {
                copy(
                    isSending = false,
                    tailMessages = tailMessages + pending,
                )
            }
            setEffect { ChatContract.Effect.ScrollToBottom }
        }
    }

    private fun shareSong(songId: String) {
        val conversationId = uiState.value.conversationId
        if (conversationId == 0 || receiverId == 0) return
        viewModelScope.launch {
            val pending = chatRepository.sendSongShare(conversationId, receiverId, songId)
            setState { copy(tailMessages = tailMessages + pending) }
            setEffect { ChatContract.Effect.ScrollToBottom }
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

    private suspend fun refreshPendingTail() {
        val conversationId = uiState.value.conversationId
        if (conversationId == 0) return
        val cached = chatRepository.getCachedMessages(conversationId)
        setState {
            copy(tailMessages = cached.filter { it.localId.contains("-") || it.status == MessageStatus.PENDING })
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

    private companion object {
        const val TYPING_IDLE_MS = 2_000L
    }
}
