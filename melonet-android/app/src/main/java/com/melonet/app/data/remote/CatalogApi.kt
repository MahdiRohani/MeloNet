package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.SongDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApi {
    @GET("api/songs/{id}")
    suspend fun getSong(@Path("id") id: String): ApiResponse<SongDto>

    @GET("api/catalog/popular")
    suspend fun getPopular(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SongDto>>

    @GET("api/catalog/new")
    suspend fun getNewReleases(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SongDto>>

    @GET("api/catalog/trending")
    suspend fun getTrending(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SongDto>>

    @GET("api/songs")
    suspend fun getSongs(
        @Query("category") category: String?,
        @Query("sort") sort: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<SongDto>>
}
