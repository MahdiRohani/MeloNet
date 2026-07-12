package com.melonet.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    @Update
    suspend fun update(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, serverId ASC")
    suspend fun getByConversation(conversationId: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): ChatMessageEntity?

    @Query("DELETE FROM chat_messages WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)

    @Query(
        """
        UPDATE chat_messages SET status = :status
        WHERE conversationId = :conversationId AND serverId IN (:messageIds)
        """,
    )
    suspend fun updateStatus(conversationId: Int, messageIds: List<Long>, status: String)

    @Query(
        """
        UPDATE chat_messages SET status = :status
        WHERE conversationId = :conversationId AND senderId = :senderId AND status != 'READ'
        """,
    )
    suspend fun updateAllFromSender(conversationId: Int, senderId: Int, status: String)
}
