package com.melonet.app.feature.auth

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<LoginContract.State, LoginContract.Event, LoginContract.Effect>() {

    override fun createInitialState() = LoginContract.State()

    override fun handleEvent(event: LoginContract.Event) {
        when (event) {
            is LoginContract.Event.LoginChanged -> setState { copy(login = event.value, error = null) }
            is LoginContract.Event.PasswordChanged -> setState { copy(password = event.value, error = null) }
            LoginContract.Event.TogglePasswordVisibility -> setState { copy(isPasswordVisible = !isPasswordVisible) }
            LoginContract.Event.Submit -> login()
            LoginContract.Event.NavigateToRegister -> setEffect { LoginContract.Effect.NavigateToRegister }
        }
    }

    private fun login() {
        val login = uiState.value.login.trim()
        val password = uiState.value.password

        if (login.isBlank() || password.isBlank()) {
            val error = AppError.Validation(code = "required_fields")
            setState { copy(error = error) }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = authRepository.login(login, password)) {
                is Result.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { LoginContract.Effect.NavigateToMain }
                }
                is Result.Error -> {
                    val error = when (result.error) {
                        AppError.Unauthorized -> AppError.Http(
                            code = "invalid_credentials",
                            httpStatus = 401,
                        )
                        else -> result.error
                    }
                    setState { copy(isLoading = false, error = error) }
                    setEffect { LoginContract.Effect.ShowError(error) }
                }
            }
        }
    }
}
