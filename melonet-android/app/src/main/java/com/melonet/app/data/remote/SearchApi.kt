package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {
    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<SearchResponseDto>
}
