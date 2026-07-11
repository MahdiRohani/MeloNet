package com.melonet.app.feature.auth

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState

object RegisterContract {

    data class State(
        val username: String = "",
        val email: String = "",
        val displayName: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isPasswordVisible: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class UsernameChanged(val value: String) : Event
        data class EmailChanged(val value: String) : Event
        data class DisplayNameChanged(val value: String) : Event
        data class PasswordChanged(val value: String) : Event
        data object TogglePasswordVisibility : Event
        data object Submit : Event
        data object NavigateToLogin : Event
    }

    sealed interface Effect : UiEffect {
        data object NavigateToMain : Effect
        data object NavigateToLogin : Effect
        data class ShowError(val message: String) : Effect
    }
}
