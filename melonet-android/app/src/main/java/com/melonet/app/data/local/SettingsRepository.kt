package com.melonet.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.melonet.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "melonet_settings")

class SettingsRepository(private val context: Context) {

    private companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
    }

    val isDarkModeFlow: Flow<Boolean?> = context.settingsDataStore.data.map { prefs ->
        prefs[IS_DARK_MODE]
    }

    val languageFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[LANGUAGE] ?: "fa"
    }

    val isPremiumFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[IS_PREMIUM] ?: false
    }

    val themeModeFlow: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        when (prefs[IS_DARK_MODE]) {
            null -> ThemeMode.SYSTEM
            true -> ThemeMode.DARK
            false -> ThemeMode.LIGHT
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            when (mode) {
                ThemeMode.SYSTEM -> prefs.remove(IS_DARK_MODE)
                ThemeMode.LIGHT -> prefs[IS_DARK_MODE] = false
                ThemeMode.DARK -> prefs[IS_DARK_MODE] = true
            }
        }
    }

    suspend fun setLanguage(lang: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[LANGUAGE] = lang
        }
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[IS_PREMIUM] = isPremium
        }
    }
}
