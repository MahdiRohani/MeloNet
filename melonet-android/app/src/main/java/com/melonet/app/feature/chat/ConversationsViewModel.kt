package com.melonet.app.feature.chat

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.Conversation
import com.melonet.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val chatRepository: ChatRepository,
) : BaseViewModel<ConversationsContract.State, ConversationsContract.Event, ConversationsContract.Effect>() {

    val conversations: Flow<PagingData<Conversation>> =
        chatRepository.conversations().cachedIn(viewModelScope)

    override fun createInitialState() = ConversationsContract.State()

    init {
        viewModelScope.launch {
            chatRepository.unreadCount.collect { count ->
                setState { copy(unreadCount = count) }
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
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            chatRepository.refreshUnreadCount()
        }
    }
}
