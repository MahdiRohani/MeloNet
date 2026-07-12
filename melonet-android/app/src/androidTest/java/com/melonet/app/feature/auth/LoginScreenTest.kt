package com.melonet.app.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.repository.AuthRepository
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginScreen_showsTitleAndSubmitButton() {
        val repository = mockk<AuthRepository>(relaxed = true)
        composeRule.setContent {
            MeloNetTheme {
                LoginScreen(
                    viewModel = LoginViewModel(repository),
                    onNavigateToMain = {},
                    onNavigateToRegister = {},
                )
            }
        }

        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in").assertIsDisplayed()
    }
}
