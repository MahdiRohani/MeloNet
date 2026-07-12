package com.melonet.app.feature.settings

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.local.SettingsRepository
import com.melonet.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : BaseViewModel<SettingsContract.State, SettingsContract.Event, SettingsContract.Effect>() {

    override fun createInitialState() = SettingsContract.State()

    init {
        settingsRepository.languageFlow
            .onEach { language -> setState { copy(language = language) } }
            .launchIn(viewModelScope)

        settingsRepository.themeModeFlow
            .onEach { themeMode -> setState { copy(themeMode = themeMode) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: SettingsContract.Event) {
        when (event) {
            is SettingsContract.Event.LanguageSelected -> setLanguage(event.language)
            is SettingsContract.Event.ThemeSelected -> setTheme(event.mode)
            SettingsContract.Event.LogoutClicked -> logout()
            SettingsContract.Event.NavigateBack -> Unit
        }
    }

    private fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            setEffect { SettingsContract.Effect.RecreateActivity }
        }
    }

    private fun setTheme(mode: com.melonet.app.data.model.ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            setEffect { SettingsContract.Effect.RecreateActivity }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            setState { copy(isLoggingOut = true) }
            authRepository.logout()
            setState { copy(isLoggingOut = false) }
            setEffect { SettingsContract.Effect.NavigateToLogin }
        }
    }
}
