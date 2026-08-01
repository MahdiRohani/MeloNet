package com.melonet.app.feature.chat

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.ChatConnectionState
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.ChatPeer
import com.melonet.app.data.model.MessageStatus

object ChatContract {
    data class ReplyDraft(
        val messageId: String,
        val author: String,
        val preview: String,
    )

    data class State(
        val isLoading: Boolean = true,
        val conversationId: Int = 0,
        val otherUser: ChatPeer? = null,
        val inputText: String = "",
        val isSending: Boolean = false,
        val isOtherTyping: Boolean = false,
        val connectionState: ChatConnectionState = ChatConnectionState.Offline,
        val statusOverrides: Map<Long, MessageStatus> = emptyMap(),
        val tailMessages: List<ChatMessage> = emptyList(),
        val replyDraft: ReplyDraft? = null,
        val error: AppError? = null,
        val shareHandled: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val otherUserId: Int, val conversationId: Int) : Event
        data class InputChanged(val value: String) : Event
        data object SendClicked : Event
        data class SongShareClicked(val songId: String) : Event
        data class MessageVisible(val message: ChatMessage) : Event
        data class RetryMessage(val localId: String) : Event
        data class CancelMessage(val localId: String) : Event
        data class CopyMessage(val text: String) : Event
        data class ReplyToMessage(val message: ChatMessage, val authorLabel: String) : Event
        data object CancelReply : Event
        data object ScreenVisible : Event
        data object ScreenHidden : Event
    }

    sealed interface Effect : UiEffect {
        data class PlaySong(val songId: String) : Effect
        data class ScrollToBottom(val force: Boolean = false) : Effect
        data class CopyToClipboard(val text: String) : Effect
    }
}
