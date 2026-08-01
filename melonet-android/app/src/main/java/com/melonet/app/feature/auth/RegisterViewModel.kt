package com.melonet.app.feature.auth

import android.util.Patterns
import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<RegisterContract.State, RegisterContract.Event, RegisterContract.Effect>() {

    override fun createInitialState() = RegisterContract.State()

    override fun handleEvent(event: RegisterContract.Event) {
        when (event) {
            is RegisterContract.Event.UsernameChanged -> setState { copy(username = event.value, error = null) }
            is RegisterContract.Event.EmailChanged -> setState { copy(email = event.value, error = null) }
            is RegisterContract.Event.DisplayNameChanged -> setState { copy(displayName = event.value, error = null) }
            is RegisterContract.Event.PasswordChanged -> setState { copy(password = event.value, error = null) }
            RegisterContract.Event.TogglePasswordVisibility -> setState { copy(isPasswordVisible = !isPasswordVisible) }
            RegisterContract.Event.Submit -> register()
            RegisterContract.Event.NavigateToLogin -> setEffect { RegisterContract.Effect.NavigateToLogin }
        }
    }

    private fun register() {
        val state = uiState.value
        val validationError = validateRegisterInput(state)
        if (validationError != null) {
            setState { copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = authRepository.register(
                username = state.username.trim(),
                email = state.email.trim(),
                password = state.password,
                displayName = state.displayName.trim(),
            )) {
                is Result.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { RegisterContract.Effect.NavigateToMain }
                }
                is Result.Error -> {
                    val message = when (val error = result.error) {
                        is AppError.Network -> mapNetworkRegisterError(error)
                        AppError.Unauthorized -> "registration_failed"
                        is AppError.Unknown -> error.message
                    }
                    setState { copy(isLoading = false, error = message) }
                    setEffect { RegisterContract.Effect.ShowError(message) }
                }
            }
        }
    }

    private fun validateRegisterInput(state: RegisterContract.State): String? {
        if (state.username.isBlank() || state.email.isBlank() ||
            state.displayName.isBlank() || state.password.isBlank()
        ) {
            return "required_fields"
        }
        if (!USERNAME_PATTERN.matches(state.username.trim())) {
            return "invalid_username"
        }
        val email = state.email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "invalid_email"
        }
        if (state.password.length < 8) {
            return "invalid_password"
        }
        return null
    }

    private fun mapNetworkRegisterError(error: AppError.Network): String {
        val msg = error.message.lowercase()
        return when {
            error.code == "user_exists" || "already exists" in msg -> "user_exists"
            "email" in msg -> "invalid_email"
            "username" in msg -> "invalid_username"
            "password" in msg -> "invalid_password"
            else -> error.message.ifBlank { "registration_failed" }
        }
    }

    companion object {
        private val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_]{3,32}$")
    }
}
