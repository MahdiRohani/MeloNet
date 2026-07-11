package com.melonet.app.data.repository

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.local.SettingsRepository
import com.melonet.app.data.local.TokenManager
import com.melonet.app.data.mapper.UserMapper
import com.melonet.app.data.model.User
import com.melonet.app.data.remote.AuthApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepository(
    private val authApi: AuthApi,
    private val settingsRepository: SettingsRepository,
    private val tokenManager: TokenManager,
    private val dispatchers: DispatchersProvider
) {
    val isPremiumFlow: Flow<Boolean> = settingsRepository.isPremiumFlow

    suspend fun getCurrentUser(): Result<User> = withContext(dispatchers.io) {
        if (tokenManager.getAccessToken().isNullOrBlank()) {
            return@withContext Result.Error(AppError.Unauthorized)
        }
        when (val result = safeApiCall { authApi.getCurrentUser() }) {
            is Result.Success -> {
                val user = UserMapper.toModel(result.data)
                settingsRepository.setPremiumStatus(user.isPremium)
                Result.Success(user)
            }
            is Result.Error -> result
        }
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        settingsRepository.setPremiumStatus(isPremium)
    }
}
