package com.melonet.app.feature.chat

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.ChatPeer
import com.melonet.app.data.model.MessageStatus

object ChatContract {
    data class State(
        val isLoading: Boolean = true,
        val conversationId: Int = 0,
        val otherUser: ChatPeer? = null,
        val inputText: String = "",
        val isSending: Boolean = false,
        val isOtherTyping: Boolean = false,
        val statusOverrides: Map<Long, MessageStatus> = emptyMap(),
        val tailMessages: List<ChatMessage> = emptyList(),
        val error: AppError? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val otherUserId: Int, val conversationId: Int) : Event
        data class InputChanged(val value: String) : Event
        data object SendClicked : Event
        data class SongShareClicked(val songId: String) : Event
        data class MessageVisible(val message: ChatMessage) : Event
        data object ScreenVisible : Event
        data object ScreenHidden : Event
    }

    sealed interface Effect : UiEffect {
        data class PlaySong(val songId: String) : Effect
        data object ScrollToBottom : Effect
    }
}
