package com.melonet.app.feature.auth

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
        if (state.username.isBlank() || state.email.isBlank() ||
            state.displayName.isBlank() || state.password.isBlank()
        ) {
            setState { copy(error = "required_fields") }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = authRepository.register(
                username = state.username,
                email = state.email,
                password = state.password,
                displayName = state.displayName,
            )) {
                is Result.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { RegisterContract.Effect.NavigateToMain }
                }
                is Result.Error -> {
                    val message = when (val error = result.error) {
                        is AppError.Network -> error.message
                        AppError.Unauthorized -> "registration_failed"
                        is AppError.Unknown -> error.message
                    }
                    setState { copy(isLoading = false, error = message) }
                    setEffect { RegisterContract.Effect.ShowError(message) }
                }
            }
        }
    }
}
