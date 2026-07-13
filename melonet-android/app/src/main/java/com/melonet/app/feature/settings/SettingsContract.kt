package com.melonet.app.feature.settings

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.ThemeMode

object SettingsContract {
    data class State(
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val isLoggingOut: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class ThemeSelected(val mode: ThemeMode) : Event
        data object LogoutClicked : Event
        data object PrivacyPolicyClicked : Event
        data object NavigateBack : Event
    }

    sealed interface Effect : UiEffect {
        data object RecreateActivity : Effect
        data object NavigateToLogin : Effect
        data class OpenPrivacyPolicy(val url: String) : Effect
    }
}
