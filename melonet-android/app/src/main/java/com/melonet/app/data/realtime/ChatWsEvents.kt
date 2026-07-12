package com.melonet.app.data.realtime

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.melonet.app.data.remote.dto.MessageDto

sealed interface ChatWsEvent {
    data class MessageAck(val clientId: String?, val message: MessageDto) : ChatWsEvent
    data class MessageNew(val message: MessageDto) : ChatWsEvent
    data class MessageDelivered(val messageId: Long, val conversationId: Int) : ChatWsEvent
    data class MessageRead(val conversationId: Int, val readerId: Int, val messageIds: List<Long>) : ChatWsEvent
    data class Typing(val conversationId: Int, val userId: Int, val isTyping: Boolean) : ChatWsEvent
    data class Error(val code: String, val message: String) : ChatWsEvent
    data object Connected : ChatWsEvent
    data object Disconnected : ChatWsEvent
}

data class WsEnvelope(
    val event: String,
    val data: JsonElement?,
)

data class WsMessageSendPayload(
    val conversation_id: Int? = null,
    val receiver_id: Int? = null,
    val content: String = "",
    val msg_type: String = "text",
    val song_id: Long? = null,
    val client_id: String? = null,
)

data class WsMessageReadPayload(
    val conversation_id: Int,
    val message_ids: List<Long>? = null,
)

data class WsTypingPayload(
    val conversation_id: Int,
    val user_id: Int? = null,
)

data class WsMessageAckPayload(
    val client_id: String?,
    val message_id: Long,
    val conversation_id: Int,
    val status: String,
)

data class WsMessageDeliveredPayload(
    val message_id: Long,
    val conversation_id: Int,
)

data class WsMessageReadNoticePayload(
    val conversation_id: Int,
    val reader_id: Int,
    val message_ids: List<Long>?,
)

data class WsErrorPayload(
    val code: String,
    val message: String,
)

object ChatWsEvents {
    const val PING = "ping"
    const val PONG = "pong"
    const val MESSAGE_SEND = "message.send"
    const val MESSAGE_NEW = "message.new"
    const val MESSAGE_ACK = "message.ack"
    const val MESSAGE_DELIVERED = "message.delivered"
    const val MESSAGE_READ = "message.read"
    const val TYPING_START = "typing.start"
    const val TYPING_STOP = "typing.stop"
    const val SONG_SHARE = "song.share"
    const val ERROR = "error"
}

object ChatWsParser {
    private val gson = Gson()

    fun parse(raw: String): ChatWsEvent? {
        val envelope = runCatching { gson.fromJson(raw, WsEnvelope::class.java) }.getOrNull() ?: return null
        return when (envelope.event) {
            ChatWsEvents.MESSAGE_ACK -> {
                val payload = decode<WsMessageAckPayload>(envelope.data) ?: return null
                val message = MessageDto(
                    id = payload.message_id,
                    conversationId = payload.conversation_id,
                    senderId = 0,
                    receiverId = 0,
                    content = "",
                    msgType = "text",
                    songId = null,
                    status = payload.status,
                    createdAt = "",
                )
                ChatWsEvent.MessageAck(payload.client_id, message)
            }
            ChatWsEvents.MESSAGE_NEW -> {
                val message = decode<MessageDto>(envelope.data) ?: return null
                ChatWsEvent.MessageNew(message)
            }
            ChatWsEvents.MESSAGE_DELIVERED -> {
                val payload = decode<WsMessageDeliveredPayload>(envelope.data) ?: return null
                ChatWsEvent.MessageDelivered(payload.message_id, payload.conversation_id)
            }
            ChatWsEvents.MESSAGE_READ -> {
                val payload = decode<WsMessageReadNoticePayload>(envelope.data) ?: return null
                ChatWsEvent.MessageRead(
                    conversationId = payload.conversation_id,
                    readerId = payload.reader_id,
                    messageIds = payload.message_ids.orEmpty(),
                )
            }
            ChatWsEvents.TYPING_START -> {
                val payload = decode<WsTypingPayload>(envelope.data) ?: return null
                ChatWsEvent.Typing(payload.conversation_id, payload.user_id ?: 0, isTyping = true)
            }
            ChatWsEvents.TYPING_STOP -> {
                val payload = decode<WsTypingPayload>(envelope.data) ?: return null
                ChatWsEvent.Typing(payload.conversation_id, payload.user_id ?: 0, isTyping = false)
            }
            ChatWsEvents.ERROR -> {
                val payload = decode<WsErrorPayload>(envelope.data) ?: return null
                ChatWsEvent.Error(payload.code, payload.message)
            }
            else -> null
        }
    }

    fun encode(event: String, data: Any?): String {
        val envelope = WsEnvelope(event = event, data = data?.let { gson.toJsonTree(it) })
        return gson.toJson(envelope)
    }

    private inline fun <reified T> decode(data: JsonElement?): T? {
        if (data == null || data.isJsonNull) return null
        return runCatching { gson.fromJson(data, T::class.java) }.getOrNull()
    }
}
