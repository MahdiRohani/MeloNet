package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.AuthTokenDto
import com.melonet.app.data.remote.dto.LoginRequestDto
import com.melonet.app.data.remote.dto.LogoutRequestDto
import com.melonet.app.data.remote.dto.LogoutResponseDto
import com.melonet.app.data.remote.dto.RefreshTokenRequestDto
import com.melonet.app.data.remote.dto.RegisterRequestDto
import com.melonet.app.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): ApiResponse<AuthTokenDto>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): ApiResponse<AuthTokenDto>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshTokenRequestDto): ApiResponse<AuthTokenDto>

    @POST("api/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto): ApiResponse<LogoutResponseDto>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): ApiResponse<UserDto>
}
