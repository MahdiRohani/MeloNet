package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.FollowResponseDto
import com.melonet.app.data.remote.dto.PlaylistDto
import com.melonet.app.data.remote.dto.PublicUserDto
import com.melonet.app.data.remote.dto.SocialUserDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SocialApi {
    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): ApiResponse<PublicUserDto>

    @GET("api/users/{id}/followers")
    suspend fun getFollowers(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SocialUserDto>>

    @GET("api/users/{id}/following")
    suspend fun getFollowing(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SocialUserDto>>

    @POST("api/users/{id}/follow")
    suspend fun follow(@Path("id") id: Int): ApiResponse<FollowResponseDto>

    @DELETE("api/users/{id}/follow")
    suspend fun unfollow(@Path("id") id: Int): ApiResponse<FollowResponseDto>

    @GET("api/users/{id}/playlists")
    suspend fun getUserPlaylists(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<PlaylistDto>>
}
