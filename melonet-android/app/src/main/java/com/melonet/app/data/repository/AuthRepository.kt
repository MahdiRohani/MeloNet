package com.melonet.app.data.repository

import com.melonet.app.core.common.AppError
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

        val cached = settingsRepository.getCachedUser()
        val provisional = cached ?: placeholderOfflineUser()
        settingsRepository.setPremiumStatus(provisional.isPremium)
        // Enter the app immediately with a cached/provisional profile so cold start
        // is not blocked by network; refine or clear after /me (or refresh) returns.
        _authState.value = AuthState.Authenticated(provisional, isOfflineSession = true)

        when (val result = safeApiCall { authApi.getCurrentUser() }) {
            is Result.Success -> {
                val user = UserMapper.toModel(result.data)
                persistAuthenticatedUser(user)
                _authState.value = AuthState.Authenticated(user, isOfflineSession = false)
            }
            is Result.Error -> handleRestoreFailure(result.error, cached)
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
                ),
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
        clearLocalSession()
    }

    suspend fun applyUser(user: User) = withContext(dispatchers.io) {
        persistAuthenticatedUser(user)
        _authState.value = AuthState.Authenticated(user, isOfflineSession = false)
    }

    suspend fun refreshTokens(): Boolean = withContext(dispatchers.io) {
        when (val outcome = attemptTokenRefresh()) {
            RefreshOutcome.Success -> true
            RefreshOutcome.AuthFailed -> {
                clearLocalSession()
                false
            }
            RefreshOutcome.TransientFailure -> false
        }
    }

    private suspend fun handleRestoreFailure(error: AppError, cached: User?) {
        when {
            error.isNetworkConnectivityError() -> {
                ensureOfflineAuthenticated(cached)
            }
            error.isAuthFailure() -> {
                when (attemptTokenRefresh()) {
                    RefreshOutcome.Success -> Unit
                    RefreshOutcome.TransientFailure -> ensureOfflineAuthenticated(cached)
                    RefreshOutcome.AuthFailed -> clearLocalSession()
                }
            }
            else -> ensureOfflineAuthenticated(cached)
        }
    }

    private suspend fun ensureOfflineAuthenticated(cached: User?) {
        if (_authState.value is AuthState.Authenticated) return
        val user = cached ?: placeholderOfflineUser()
        settingsRepository.setPremiumStatus(user.isPremium)
        _authState.value = AuthState.Authenticated(user, isOfflineSession = true)
    }

    private suspend fun attemptTokenRefresh(): RefreshOutcome {
        val refreshToken = tokenManager.getRefreshToken() ?: return RefreshOutcome.AuthFailed
        return when (val result = safeApiCall {
            authApi.refresh(RefreshTokenRequestDto(refreshToken))
        }) {
            is Result.Success -> {
                handleAuthSuccess(result.data)
                RefreshOutcome.Success
            }
            is Result.Error -> when {
                result.error.isNetworkConnectivityError() -> RefreshOutcome.TransientFailure
                result.error.isAuthFailure() -> RefreshOutcome.AuthFailed
                else -> RefreshOutcome.TransientFailure
            }
        }
    }

    private suspend fun handleAuthSuccess(dto: AuthTokenDto): User {
        tokenManager.saveTokens(dto.accessToken, dto.refreshToken)
        val user = UserMapper.toModel(dto.user)
        persistAuthenticatedUser(user)
        _authState.value = AuthState.Authenticated(user, isOfflineSession = false)
        return user
    }

    private suspend fun persistAuthenticatedUser(user: User) {
        settingsRepository.saveCachedUser(user)
    }

    private suspend fun clearLocalSession() {
        tokenManager.clearTokens()
        settingsRepository.clearCachedUser()
        _authState.value = AuthState.Unauthenticated
    }

    private fun placeholderOfflineUser(): User = User(
        id = 0,
        username = "",
        displayName = "Offline",
        email = "",
        avatarUrl = "",
        bio = "",
        isPremium = false,
    )

    private enum class RefreshOutcome {
        Success,
        AuthFailed,
        TransientFailure,
    }
}

private fun AppError.isNetworkConnectivityError(): Boolean =
    this is AppError.Timeout || this is AppError.NoConnection

private fun AppError.isAuthFailure(): Boolean = when (this) {
    AppError.Unauthorized, AppError.Forbidden -> true
    is AppError.Http -> httpStatus == 401 || httpStatus == 403
    else -> false
}
