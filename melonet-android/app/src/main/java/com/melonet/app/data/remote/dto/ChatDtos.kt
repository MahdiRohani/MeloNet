package com.melonet.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ConversationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("other_user") val otherUser: ChatUserDto?,
    @SerializedName("last_message") val lastMessage: MessageDto?,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("updated_at") val updatedAt: String,
)

data class ChatUserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("is_premium") val isPremium: Boolean,
)

data class MessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("conversation_id") val conversationId: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("msg_type") val msgType: String,
    @SerializedName("song_id") val songId: Long?,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
)

data class CreateConversationRequestDto(
    @SerializedName("user_id") val userId: Int,
)

data class UnreadCountDto(
    @SerializedName("total") val total: Int,
)

data class MarkReadRequestDto(
    @SerializedName("message_ids") val messageIds: List<Long>,
)
