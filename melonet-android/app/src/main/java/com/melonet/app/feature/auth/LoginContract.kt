package com.melonet.app.feature.auth

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState

object LoginContract {

    data class State(
        val login: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isPasswordVisible: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class LoginChanged(val value: String) : Event
        data class PasswordChanged(val value: String) : Event
        data object TogglePasswordVisibility : Event
        data object Submit : Event
        data object NavigateToRegister : Event
    }

    sealed interface Effect : UiEffect {
        data object NavigateToMain : Effect
        data object NavigateToRegister : Effect
        data class ShowError(val message: String) : Effect
    }
}
