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
import com.melonet.app.feature.chat.SongShareCodec
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

    fun toMessage(dto: MessageDto, currentUserId: Int): ChatMessage {
        val attachment = SongShareCodec.parse(dto.content)
        return ChatMessage(
            localId = "server_${dto.id}",
            serverId = dto.id,
            conversationId = dto.conversationId,
            senderId = dto.senderId,
            receiverId = dto.receiverId,
            content = dto.content,
            msgType = MessageType.fromApi(dto.msgType),
            songId = dto.songId?.takeIf { it.isNotBlank() },
            status = MessageStatus.fromApi(dto.status),
            createdAt = parseInstant(dto.createdAt),
            isMine = currentUserId > 0 && dto.senderId == currentUserId,
            songTitle = attachment?.title?.takeIf { it.isNotBlank() },
            songArtist = attachment?.artist?.takeIf { it.isNotBlank() },
            songCoverUrl = attachment?.cover?.takeIf { it.isNotBlank() },
            songAudioUrl = attachment?.audioUrl,
        )
    }

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

    fun fromEntity(entity: ChatMessageEntity, currentUserId: Int): ChatMessage {
        val attachment = SongShareCodec.parse(entity.content)
        return ChatMessage(
            localId = entity.localId,
            serverId = entity.serverId,
            conversationId = entity.conversationId,
            senderId = entity.senderId,
            receiverId = entity.receiverId,
            content = entity.content,
            msgType = MessageType.valueOf(entity.msgType),
            songId = entity.songId,
            status = runCatching { MessageStatus.valueOf(entity.status) }.getOrDefault(MessageStatus.SENT),
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            isMine = currentUserId > 0 && entity.senderId == currentUserId,
            songTitle = entity.songTitle ?: attachment?.title?.takeIf { it.isNotBlank() },
            songArtist = entity.songArtist ?: attachment?.artist?.takeIf { it.isNotBlank() },
            songCoverUrl = entity.songCoverUrl ?: attachment?.cover?.takeIf { it.isNotBlank() },
            songAudioUrl = attachment?.audioUrl,
        )
    }

    private fun parseInstant(value: String): Instant {
        if (value.isBlank()) return Instant.now()
        return runCatching {
            Instant.from(isoFormatter.parse(value))
        }.getOrElse {
            runCatching { Instant.parse(value) }.getOrDefault(Instant.now())
        }
    }
}
