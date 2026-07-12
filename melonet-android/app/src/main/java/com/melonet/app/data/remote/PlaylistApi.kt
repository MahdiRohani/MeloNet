package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.AddPlaylistSongRequestDto
import com.melonet.app.data.remote.dto.CreatePlaylistRequestDto
import com.melonet.app.data.remote.dto.PlaylistDto
import com.melonet.app.data.remote.dto.SongDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaylistApi {
    @GET("api/playlists")
    suspend fun getPlaylists(
        @Query("scope") scope: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<PlaylistDto>>

    @GET("api/playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: Int): ApiResponse<PlaylistDto>

    @POST("api/playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequestDto): ApiResponse<PlaylistDto>

    @DELETE("api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: Int): ApiResponse<Unit>

    @GET("api/playlists/{id}/songs")
    suspend fun getPlaylistSongs(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SongDto>>
}
