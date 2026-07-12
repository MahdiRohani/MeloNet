package com.melonet.app.data.mapper

import com.melonet.app.data.model.MessageStatus
import com.melonet.app.data.model.MessageType
import com.melonet.app.data.remote.dto.MessageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMapperTest {

    @Test
    fun toMessage_mapsFieldsCorrectly() {
        val dto = MessageDto(
            id = 42,
            conversationId = 7,
            senderId = 1,
            receiverId = 2,
            content = "Hello",
            msgType = "text",
            songId = null,
            status = "delivered",
            createdAt = "2026-01-15T10:30:00Z",
        )

        val message = ChatMapper.toMessage(dto, currentUserId = 1)

        assertEquals(42L, message.serverId)
        assertEquals(7, message.conversationId)
        assertEquals("Hello", message.content)
        assertEquals(MessageType.TEXT, message.msgType)
        assertEquals(MessageStatus.DELIVERED, message.status)
        assertTrue(message.isMine)
    }

    @Test
    fun toMessage_songType_parsesSongId() {
        val dto = MessageDto(
            id = 5,
            conversationId = 1,
            senderId = 2,
            receiverId = 1,
            content = "",
            msgType = "song",
            songId = 99,
            status = "sent",
            createdAt = "2026-01-15T10:30:00Z",
        )

        val message = ChatMapper.toMessage(dto, currentUserId = 1)

        assertEquals(MessageType.SONG, message.msgType)
        assertEquals("99", message.songId)
        assertEquals(false, message.isMine)
    }
}
