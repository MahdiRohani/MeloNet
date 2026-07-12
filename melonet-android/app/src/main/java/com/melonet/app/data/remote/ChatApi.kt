package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.ConversationDto
import com.melonet.app.data.remote.dto.CreateConversationRequestDto
import com.melonet.app.data.remote.dto.MarkReadRequestDto
import com.melonet.app.data.remote.dto.MessageDto
import com.melonet.app.data.remote.dto.UnreadCountDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("api/conversations")
    suspend fun listConversations(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<ConversationDto>>

    @POST("api/conversations")
    suspend fun createConversation(
        @Body body: CreateConversationRequestDto,
    ): ApiResponse<ConversationDto>

    @GET("api/conversations/{id}")
    suspend fun getConversation(
        @Path("id") conversationId: Int,
    ): ApiResponse<ConversationDto>

    @GET("api/conversations/unread-count")
    suspend fun unreadCount(): ApiResponse<UnreadCountDto>

    @GET("api/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<MessageDto>>

    @POST("api/conversations/{id}/read")
    suspend fun markRead(
        @Path("id") conversationId: Int,
        @Body body: MarkReadRequestDto,
    ): ApiResponse<Map<String, Boolean>>
}
