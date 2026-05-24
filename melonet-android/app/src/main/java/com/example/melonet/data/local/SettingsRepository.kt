package com.example.melonet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "melonet_settings")

class SettingsRepository(private val context: Context) {


    private companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
    }


    val isDarkModeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE]
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: "fa"
    }

    val isPremiumFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PREMIUM] ?: false
    }



    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = lang
        }
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PREMIUM] = isPremium
        }
    }
}