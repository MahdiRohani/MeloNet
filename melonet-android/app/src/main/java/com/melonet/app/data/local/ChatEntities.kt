package com.melonet.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index("conversationId"), Index("serverId")],
)
data class ChatMessageEntity(
    @PrimaryKey val localId: String,
    val serverId: Long?,
    val conversationId: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val msgType: String,
    val songId: String?,
    val status: String,
    val createdAt: Long,
    val songTitle: String? = null,
    val songArtist: String? = null,
    val songCoverUrl: String? = null,
)
