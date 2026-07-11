package com.melonet.app.domain.repository

import com.melonet.app.core.common.Result
import com.melonet.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val isPremiumFlow: Flow<Boolean>

    suspend fun getCurrentUser(): Result<User>
    suspend fun setPremiumStatus(isPremium: Boolean)
}
