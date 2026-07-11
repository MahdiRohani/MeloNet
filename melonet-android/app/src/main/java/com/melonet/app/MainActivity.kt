package com.melonet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.melonet.app.core.designsystem.ProvideAppLocale
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.navigation.MelonetMainScreen
import com.melonet.app.data.local.SettingsRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkModePref by settingsRepository.isDarkModeFlow.collectAsState(initial = null)
            val language by settingsRepository.languageFlow.collectAsState(initial = "fa")
            val isDarkTheme = darkModePref ?: isSystemInDarkTheme()

            ProvideAppLocale(language = language) {
                MeloNetTheme(darkTheme = isDarkTheme) {
                    MelonetMainScreen()
                }
            }
        }
    }
}
