package com.melonet.app.data.remote

import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.remote.dto.AuthTokenDto
import com.melonet.app.data.remote.dto.LoginRequestDto
import com.melonet.app.data.remote.dto.LogoutRequestDto
import com.melonet.app.data.remote.dto.LogoutResponseDto
import com.melonet.app.data.remote.dto.RefreshTokenRequestDto
import com.melonet.app.data.remote.dto.RegisterRequestDto
import com.melonet.app.data.remote.dto.UpdateProfileRequestDto
import com.melonet.app.data.remote.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

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

    @PATCH("api/auth/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequestDto): ApiResponse<UserDto>

    @Multipart
    @POST("api/auth/me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): ApiResponse<UserDto>
}
