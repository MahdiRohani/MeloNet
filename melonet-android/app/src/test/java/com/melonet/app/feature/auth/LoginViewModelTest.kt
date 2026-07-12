package com.melonet.app.feature.auth

import com.melonet.app.data.repository.AuthRepository
import com.melonet.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submitWithEmptyFields_setsRequiredError() = runTest {
        val repository = mockk<AuthRepository>()
        val viewModel = LoginViewModel(repository)

        viewModel.handleEvent(LoginContract.Event.Submit)

        assertEquals("required_fields", viewModel.uiState.value.error)
    }

    @Test
    fun loginChanged_clearsError() = runTest {
        val repository = mockk<AuthRepository>()
        val viewModel = LoginViewModel(repository)

        viewModel.handleEvent(LoginContract.Event.Submit)
        viewModel.handleEvent(LoginContract.Event.LoginChanged("mahdi"))

        assertEquals(null, viewModel.uiState.value.error)
        assertEquals("mahdi", viewModel.uiState.value.login)
    }

    @Test
    fun togglePasswordVisibility_flipsFlag() = runTest {
        val repository = mockk<AuthRepository>()
        val viewModel = LoginViewModel(repository)

        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.handleEvent(LoginContract.Event.TogglePasswordVisibility)
        assertTrue(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun submitWithInvalidCredentials_setsError() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery { repository.login("mahdi", "wrong") } returns com.melonet.app.core.common.Result.Error(
            com.melonet.app.core.common.AppError.Unauthorized,
        )
        val viewModel = LoginViewModel(repository)

        viewModel.handleEvent(LoginContract.Event.LoginChanged("mahdi"))
        viewModel.handleEvent(LoginContract.Event.PasswordChanged("wrong"))
        viewModel.handleEvent(LoginContract.Event.Submit)

        assertEquals("invalid_credentials", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
