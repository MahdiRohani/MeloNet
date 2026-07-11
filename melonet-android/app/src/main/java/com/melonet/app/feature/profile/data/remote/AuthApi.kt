package com.melonet.app.feature.profile.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.feature.profile.data.dto.UserDto
import retrofit2.http.GET

interface AuthApi {
    @GET("api/auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserDto>
}
