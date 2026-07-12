package com.melonet.app.data.model

import java.time.Instant

enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ,
    ;

    companion object {
        fun fromApi(value: String): MessageStatus = when (value.lowercase()) {
            "sent" -> SENT
            "delivered" -> DELIVERED
            "read" -> READ
            else -> SENT
        }
    }
}

enum class MessageType {
    TEXT,
    SONG,
    IMAGE,
    SYSTEM,
    ;

    companion object {
        fun fromApi(value: String): MessageType = when (value.lowercase()) {
            "song" -> SONG
            "image" -> IMAGE
            "system" -> SYSTEM
            else -> TEXT
        }

        fun toApi(value: MessageType): String = when (value) {
            TEXT -> "text"
            SONG -> "song"
            IMAGE -> "image"
            SYSTEM -> "system"
        }
    }
}

data class ChatPeer(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val isPremium: Boolean,
)

data class Conversation(
    val id: Int,
    val otherUser: ChatPeer,
    val lastMessage: ChatMessage?,
    val unreadCount: Int,
    val updatedAt: Instant,
)

data class ChatMessage(
    val localId: String,
    val serverId: Long?,
    val conversationId: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val msgType: MessageType,
    val songId: String?,
    val status: MessageStatus,
    val createdAt: Instant,
    val isMine: Boolean,
    val songTitle: String? = null,
    val songArtist: String? = null,
    val songCoverUrl: String? = null,
) {
    val stableKey: String = serverId?.toString() ?: localId
}
