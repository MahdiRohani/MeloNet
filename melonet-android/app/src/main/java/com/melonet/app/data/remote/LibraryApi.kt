package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.PlayEventRequestDto
import com.melonet.app.data.remote.dto.PlayEventResponseDto
import com.melonet.app.data.remote.dto.SongDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LibraryApi {
    @GET("api/library/liked")
    suspend fun getLikedSongs(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SongDto>>

    @GET("api/library/recent")
    suspend fun getRecentSongs(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SongDto>>

    @DELETE("api/songs/{id}/like")
    suspend fun unlikeSong(@Path("id") id: Int): ApiResponse<Unit>

    @POST("api/songs/{id}/play")
    suspend fun recordPlay(
        @Path("id") id: Int,
        @Body request: PlayEventRequestDto,
    ): ApiResponse<PlayEventResponseDto>
}
