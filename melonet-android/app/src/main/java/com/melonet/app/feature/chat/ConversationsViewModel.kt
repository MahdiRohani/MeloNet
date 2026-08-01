package com.melonet.app.feature.chat

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.Conversation
import com.melonet.app.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModel(
    private val chatRepository: ChatRepository,
) : BaseViewModel<ConversationsContract.State, ConversationsContract.Event, ConversationsContract.Effect>() {

    private val refreshTrigger = MutableStateFlow(0)

    val conversations: Flow<PagingData<Conversation>> = refreshTrigger
        .flatMapLatest { chatRepository.conversations() }
        .cachedIn(viewModelScope)

    override fun createInitialState() = ConversationsContract.State()

    init {
        viewModelScope.launch {
            chatRepository.unreadCount.collect { count ->
                setState { copy(unreadCount = count) }
            }
        }
        viewModelScope.launch {
            chatRepository.connectionState.collect { state ->
                setState { copy(connectionState = state) }
            }
        }
        viewModelScope.launch {
            chatRepository.conversationUpdates.collect {
                invalidateConversations()
                chatRepository.refreshUnreadCount()
            }
        }
    }

    override fun handleEvent(event: ConversationsContract.Event) {
        when (event) {
            ConversationsContract.Event.ScreenVisible -> refresh()
            is ConversationsContract.Event.ConversationClicked -> {
                setEffect {
                    ConversationsContract.Effect.NavigateToChat(
                        conversationId = event.conversation.id,
                        otherUserId = event.conversation.otherUser.id,
                        otherDisplayName = event.conversation.otherUser.displayName,
                    )
                }
            }
            is ConversationsContract.Event.SearchChanged -> {
                setState { copy(searchQuery = event.query) }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            chatRepository.refreshUnreadCount()
            invalidateConversations()
        }
    }

    private fun invalidateConversations() {
        refreshTrigger.value = refreshTrigger.value + 1
        setState { copy(refreshKey = refreshTrigger.value) }
        setEffect { ConversationsContract.Effect.RefreshList }
    }
}
