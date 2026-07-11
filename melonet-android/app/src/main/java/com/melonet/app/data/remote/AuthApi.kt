package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.UserDto
import retrofit2.http.GET

interface AuthApi {
    @GET("api/auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserDto>
}
