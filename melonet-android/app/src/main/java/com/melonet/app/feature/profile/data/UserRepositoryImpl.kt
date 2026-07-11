package com.melonet.app.feature.profile.data

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.datastore.SettingsRepository
import com.melonet.app.core.network.TokenManager
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.domain.model.User
import com.melonet.app.domain.repository.UserRepository
import com.melonet.app.feature.profile.data.mapper.UserMapper
import com.melonet.app.feature.profile.data.remote.AuthApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val authApi: AuthApi,
    private val settingsRepository: SettingsRepository,
    private val tokenManager: TokenManager,
    private val dispatchers: DispatchersProvider
) : UserRepository {

    override val isPremiumFlow: Flow<Boolean> = settingsRepository.isPremiumFlow

    override suspend fun getCurrentUser(): Result<User> = withContext(dispatchers.io) {
        if (tokenManager.getAccessToken().isNullOrBlank()) {
            return@withContext Result.Error(AppError.Unauthorized)
        }
        safeApiCall { authApi.getCurrentUser() }.let { result ->
            when (result) {
                is Result.Success -> {
                    val user = UserMapper.toDomain(result.data)
                    settingsRepository.setPremiumStatus(user.isPremium)
                    Result.Success(user)
                }
                is Result.Error -> result
            }
        }
    }

    override suspend fun setPremiumStatus(isPremium: Boolean) {
        settingsRepository.setPremiumStatus(isPremium)
    }
}
