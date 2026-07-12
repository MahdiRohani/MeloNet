package com.melonet.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthTokenDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("access_expires_at") val accessExpiresAt: String?,
    @SerializedName("refresh_expires_at") val refreshExpiresAt: String?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("user") val user: UserDto,
)

data class LoginRequestDto(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String,
)

data class RegisterRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("display_name") val displayName: String,
)

data class RefreshTokenRequestDto(
    @SerializedName("refresh_token") val refreshToken: String,
)

data class LogoutRequestDto(
    @SerializedName("refresh_token") val refreshToken: String,
)

data class LogoutResponseDto(
    @SerializedName("logged_out") val loggedOut: Boolean,
)

data class UpdateProfileRequestDto(
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("email") val email: String? = null,
)
