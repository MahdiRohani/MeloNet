package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.CreateVoiceCoverRequestDto
import com.melonet.app.data.remote.dto.DeletedDto
import com.melonet.app.data.remote.dto.VoiceArtistDto
import com.melonet.app.data.remote.dto.VoiceCoverDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VoiceCoverApi {
    @GET("api/voice-covers/artists")
    suspend fun listArtists(): ApiResponse<List<VoiceArtistDto>>

    @GET("api/voice-covers")
    suspend fun listReady(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<VoiceCoverDto>>

    @POST("api/voice-covers")
    suspend fun create(@Body body: CreateVoiceCoverRequestDto): ApiResponse<VoiceCoverDto>

    @GET("api/voice-covers/{id}")
    suspend fun get(@Path("id") id: Long): ApiResponse<VoiceCoverDto>

    @DELETE("api/voice-covers/{id}")
    suspend fun delete(@Path("id") id: Long): ApiResponse<DeletedDto>
}
