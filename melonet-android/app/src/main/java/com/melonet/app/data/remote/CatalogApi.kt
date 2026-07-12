package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.SongDto
import retrofit2.http.GET
import retrofit2.http.Path

interface CatalogApi {
    @GET("api/songs/{id}")
    suspend fun getSong(@Path("id") id: String): ApiResponse<SongDto>
}
