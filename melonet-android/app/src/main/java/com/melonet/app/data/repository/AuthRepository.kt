package com.melonet.app.data.repository

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.local.SettingsRepository
import com.melonet.app.data.local.TokenManager
import com.melonet.app.data.mapper.UserMapper
import com.melonet.app.data.model.AuthState
import com.melonet.app.data.model.User
import com.melonet.app.data.remote.AuthApi
import com.melonet.app.data.remote.dto.AuthTokenDto
import com.melonet.app.data.remote.dto.LoginRequestDto
import com.melonet.app.data.remote.dto.LogoutRequestDto
import com.melonet.app.data.remote.dto.RefreshTokenRequestDto
import com.melonet.app.data.remote.dto.RegisterRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatchersProvider,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun restoreSession() = withContext(dispatchers.io) {
        _authState.value = AuthState.Loading
        if (tokenManager.getAccessToken().isNullOrBlank()) {
            _authState.value = AuthState.Unauthenticated
            return@withContext
        }
        when (val result = safeApiCall { authApi.getCurrentUser() }) {
            is Result.Success -> {
                val user = UserMapper.toModel(result.data)
                settingsRepository.setPremiumStatus(user.isPremium)
                _authState.value = AuthState.Authenticated(user)
            }
            is Result.Error -> {
                tokenManager.clearTokens()
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    suspend fun login(login: String, password: String): Result<User> = withContext(dispatchers.io) {
        when (val result = safeApiCall {
            authApi.login(LoginRequestDto(login = login.trim(), password = password))
        }) {
            is Result.Success -> Result.Success(handleAuthSuccess(result.data))
            is Result.Error -> result
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String,
    ): Result<User> = withContext(dispatchers.io) {
        when (val result = safeApiCall {
            authApi.register(
                RegisterRequestDto(
                    username = username.trim(),
                    email = email.trim(),
                    password = password,
                    displayName = displayName.trim(),
                )
            )
        }) {
            is Result.Success -> Result.Success(handleAuthSuccess(result.data))
            is Result.Error -> result
        }
    }

    suspend fun logout() = withContext(dispatchers.io) {
        val refreshToken = tokenManager.getRefreshToken()
        if (!refreshToken.isNullOrBlank()) {
            safeApiCall { authApi.logout(LogoutRequestDto(refreshToken)) }
        }
        tokenManager.clearTokens()
        _authState.value = AuthState.Unauthenticated
    }

    suspend fun refreshTokens(): Boolean = withContext(dispatchers.io) {
        val refreshToken = tokenManager.getRefreshToken() ?: return@withContext false
        when (val result = safeApiCall {
            authApi.refresh(RefreshTokenRequestDto(refreshToken))
        }) {
            is Result.Success -> {
                handleAuthSuccess(result.data)
                true
            }
            is Result.Error -> {
                tokenManager.clearTokens()
                _authState.value = AuthState.Unauthenticated
                false
            }
        }
    }

    private suspend fun handleAuthSuccess(dto: AuthTokenDto): User {
        tokenManager.saveTokens(dto.accessToken, dto.refreshToken)
        val user = UserMapper.toModel(dto.user)
        settingsRepository.setPremiumStatus(user.isPremium)
        _authState.value = AuthState.Authenticated(user)
        return user
    }
}
