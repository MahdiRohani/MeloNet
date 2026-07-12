package com.melonet.app.feature.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloFilterChip
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsContract.Effect.RecreateActivity -> {
                    (context as? Activity)?.recreate()
                }
                SettingsContract.Effect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_player_back),
                    )
                }
            },
        )

        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SettingsSection(title = stringResource(R.string.settings_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    MeloFilterChip(
                        label = stringResource(R.string.settings_language_fa),
                        selected = state.language == "fa",
                        onClick = { viewModel.handleEvent(SettingsContract.Event.LanguageSelected("fa")) },
                    )
                    MeloFilterChip(
                        label = stringResource(R.string.settings_language_en),
                        selected = state.language == "en",
                        onClick = { viewModel.handleEvent(SettingsContract.Event.LanguageSelected("en")) },
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.settings_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    MeloFilterChip(
                        label = stringResource(R.string.settings_theme_system),
                        selected = state.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.handleEvent(SettingsContract.Event.ThemeSelected(ThemeMode.SYSTEM)) },
                    )
                    MeloFilterChip(
                        label = stringResource(R.string.settings_theme_light),
                        selected = state.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.handleEvent(SettingsContract.Event.ThemeSelected(ThemeMode.LIGHT)) },
                    )
                    MeloFilterChip(
                        label = stringResource(R.string.settings_theme_dark),
                        selected = state.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.handleEvent(SettingsContract.Event.ThemeSelected(ThemeMode.DARK)) },
                    )
                }
            }

            MeloButton(
                text = stringResource(R.string.settings_logout),
                onClick = { viewModel.handleEvent(SettingsContract.Event.LogoutClicked) },
                enabled = !state.isLoggingOut,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
    }
}
