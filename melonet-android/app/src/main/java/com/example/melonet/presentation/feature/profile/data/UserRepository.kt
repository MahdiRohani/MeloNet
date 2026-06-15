package com.example.melonet.presentation.feature.profile.data



import com.example.melonet.data.local.SettingsRepository
import com.example.melonet.data.remote.MeloNetApi
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val api: MeloNetApi,
    private val settingsRepository: SettingsRepository
) {
    val isPremiumFlow: Flow<Boolean> = settingsRepository.isPremiumFlow

    suspend fun upgradeToPremium() {

        settingsRepository.setPremiumStatus(true)
        // api.updateUserStatus(isPremium = true)
    }
}