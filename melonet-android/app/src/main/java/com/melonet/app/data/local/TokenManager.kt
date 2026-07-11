package com.melonet.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "melonet_tokens")

class TokenManager(private val context: Context) {

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val isAuthenticated: Flow<Boolean> = context.tokenDataStore.data.map { prefs ->
        !prefs[ACCESS_TOKEN].isNullOrBlank()
    }

    suspend fun getAccessToken(): String? =
        context.tokenDataStore.data.first()[ACCESS_TOKEN]

    suspend fun getRefreshToken(): String? =
        context.tokenDataStore.data.first()[REFRESH_TOKEN]

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clearTokens() {
        context.tokenDataStore.edit { it.clear() }
    }
}
