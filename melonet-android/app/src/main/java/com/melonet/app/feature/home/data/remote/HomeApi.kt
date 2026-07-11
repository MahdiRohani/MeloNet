package com.melonet.app.feature.home.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.feature.home.data.dto.HomeFeedDto
import retrofit2.http.GET

interface HomeApi {
    @GET("api/home")
    suspend fun getHomeFeed(): ApiResponse<HomeFeedDto>
}
