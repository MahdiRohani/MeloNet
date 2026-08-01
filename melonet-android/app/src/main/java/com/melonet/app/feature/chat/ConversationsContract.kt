package com.melonet.app.feature.chat

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.ChatConnectionState
import com.melonet.app.data.model.Conversation

object ConversationsContract {
    data class State(
        val unreadCount: Int = 0,
        val connectionState: ChatConnectionState = ChatConnectionState.Offline,
        val searchQuery: String = "",
        val refreshKey: Int = 0,
        val error: AppError? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object ScreenVisible : Event
        data class ConversationClicked(val conversation: Conversation) : Event
        data class SearchChanged(val query: String) : Event
    }

    sealed interface Effect : UiEffect {
        data class NavigateToChat(
            val conversationId: Int,
            val otherUserId: Int,
            val otherDisplayName: String,
        ) : Effect
        data object RefreshList : Effect
    }
}
