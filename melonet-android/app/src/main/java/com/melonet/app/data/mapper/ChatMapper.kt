package com.melonet.app.data.mapper

import com.melonet.app.data.local.ChatMessageEntity
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.model.ChatPeer
import com.melonet.app.data.model.Conversation
import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.model.MessageType
import com.melonet.app.data.remote.dto.ChatUserDto
import com.melonet.app.data.remote.dto.ConversationDto
import com.melonet.app.data.remote.dto.MessageDto
import java.time.Instant
import java.time.format.DateTimeFormatter

object ChatMapper {
    private val isoFormatter = DateTimeFormatter.ISO_DATE_TIME

    fun toConversation(dto: ConversationDto): Conversation {
        val other = dto.otherUser ?: ChatUserDto(
            id = 0,
            username = "",
            displayName = "",
            avatarUrl = null,
            bio = null,
            isPremium = false,
        )
        return Conversation(
            id = dto.id,
            otherUser = toPeer(other),
            lastMessage = dto.lastMessage?.let { toMessage(it, currentUserId = 0) },
            unreadCount = dto.unreadCount,
            updatedAt = parseInstant(dto.updatedAt),
        )
    }

    fun toPeer(dto: ChatUserDto): ChatPeer = ChatPeer(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
        avatarUrl = dto.avatarUrl,
        isPremium = dto.isPremium,
    )

    fun toMessage(dto: MessageDto, currentUserId: Int): ChatMessage = ChatMessage(
        localId = "server_${dto.id}",
        serverId = dto.id,
        conversationId = dto.conversationId,
        senderId = dto.senderId,
        receiverId = dto.receiverId,
        content = dto.content,
        msgType = MessageType.fromApi(dto.msgType),
        songId = dto.songId?.takeIf { it > 0 }?.toString(),
        status = MessageStatus.fromApi(dto.status),
        createdAt = parseInstant(dto.createdAt),
        isMine = currentUserId > 0 && dto.senderId == currentUserId,
    )

    fun toEntity(message: ChatMessage): ChatMessageEntity = ChatMessageEntity(
        localId = message.localId,
        serverId = message.serverId,
        conversationId = message.conversationId,
        senderId = message.senderId,
        receiverId = message.receiverId,
        content = message.content,
        msgType = message.msgType.name,
        songId = message.songId,
        status = message.status.name,
        createdAt = message.createdAt.toEpochMilli(),
        songTitle = message.songTitle,
        songArtist = message.songArtist,
        songCoverUrl = message.songCoverUrl,
    )

    fun fromEntity(entity: ChatMessageEntity, currentUserId: Int): ChatMessage = ChatMessage(
        localId = entity.localId,
        serverId = entity.serverId,
        conversationId = entity.conversationId,
        senderId = entity.senderId,
        receiverId = entity.receiverId,
        content = entity.content,
        msgType = MessageType.valueOf(entity.msgType),
        songId = entity.songId,
        status = MessageStatus.valueOf(entity.status),
        createdAt = Instant.ofEpochMilli(entity.createdAt),
        isMine = currentUserId > 0 && entity.senderId == currentUserId,
        songTitle = entity.songTitle,
        songArtist = entity.songArtist,
        songCoverUrl = entity.songCoverUrl,
    )

    private fun parseInstant(value: String): Instant = runCatching {
        Instant.from(isoFormatter.parse(value))
    }.getOrElse {
        runCatching { Instant.parse(value) }.getOrDefault(Instant.EPOCH)
    }
}
