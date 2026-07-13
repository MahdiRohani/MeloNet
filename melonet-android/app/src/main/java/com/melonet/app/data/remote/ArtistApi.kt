package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.ArtistDto
import com.melonet.app.data.remote.dto.SongDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ArtistApi {
    @GET("api/artists")
    suspend fun getArtists(
        @Query("region") region: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<ArtistDto>>

    @GET("api/artists/{id}")
    suspend fun getArtist(@Path("id") id: Int): ApiResponse<ArtistDto>

    @GET("api/artists/{id}/songs")
    suspend fun getArtistSongs(
        @Path("id") id: Int,
        @Query("sort") sort: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SongDto>>

    @POST("api/artists/{id}/follow")
    suspend fun follow(@Path("id") id: Int): ApiResponse<ArtistDto>

    @DELETE("api/artists/{id}/follow")
    suspend fun unfollow(@Path("id") id: Int): ApiResponse<ArtistDto>

    @GET("api/me/following/artists")
    suspend fun getFollowedArtists(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<ArtistDto>>
}
