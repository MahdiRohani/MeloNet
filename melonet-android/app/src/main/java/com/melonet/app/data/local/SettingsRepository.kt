package com.melonet.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.melonet.app.data.model.ThemeMode
import com.melonet.app.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "melonet_settings")

class SettingsRepository(private val context: Context) {

    private companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val CACHED_USER_ID = intPreferencesKey("cached_user_id")
        val CACHED_USERNAME = stringPreferencesKey("cached_username")
        val CACHED_DISPLAY_NAME = stringPreferencesKey("cached_display_name")
        val CACHED_EMAIL = stringPreferencesKey("cached_email")
        val CACHED_AVATAR_URL = stringPreferencesKey("cached_avatar_url")
        val CACHED_BIO = stringPreferencesKey("cached_bio")
        val CACHED_IS_PREMIUM = booleanPreferencesKey("cached_is_premium")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_NAME = stringPreferencesKey("eq_preset_name")
        val EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val BUBBLE_X = stringPreferencesKey("bubble_offset_x")
        val BUBBLE_Y = stringPreferencesKey("bubble_offset_y")
        val BUBBLE_HAS_POS = booleanPreferencesKey("bubble_has_pos")
        val CHAT_HISTORY_WIPED_V1 = booleanPreferencesKey("chat_history_wiped_v1")
    }

    val isDarkModeFlow: Flow<Boolean?> = context.settingsDataStore.data.map { prefs ->
        prefs[IS_DARK_MODE]
    }

    val languageFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[LANGUAGE] ?: "en"
    }

    val isPremiumFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[CACHED_IS_PREMIUM] ?: prefs[IS_PREMIUM] ?: false
    }

    /** Crossfade duration in seconds: 0, 2, 5, or 8. Default 3s ≈ mid option. */
    val crossfadeSecondsFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        (prefs[CROSSFADE_SECONDS] ?: 3).coerceIn(0, 8)
    }

    val equalizerSettingsFlow: Flow<EqualizerSettings> = context.settingsDataStore.data.map { prefs ->
        EqualizerSettings(
            enabled = prefs[EQ_ENABLED] ?: true,
            usePreset = false,
            presetIndex = -1,
            bandLevelsMilliBel = prefs[EQ_BAND_LEVELS]
                ?.split(',')
                ?.mapNotNull { it.trim().toIntOrNull() }
                .orEmpty(),
            bassBoostStrength = prefs[EQ_BASS_BOOST] ?: 0,
            virtualizerStrength = prefs[EQ_VIRTUALIZER] ?: 0,
            selectedPresetName = prefs[EQ_PRESET_NAME] ?: "Normal",
        )
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
            prefs[CACHED_IS_PREMIUM] = isPremium
        }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[CROSSFADE_SECONDS] = seconds.coerceIn(0, 8)
        }
    }

    suspend fun getCrossfadeSeconds(): Int =
        (context.settingsDataStore.data.first()[CROSSFADE_SECONDS] ?: 3).coerceIn(0, 8)

    suspend fun saveEqualizerSettings(settings: EqualizerSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[EQ_ENABLED] = settings.enabled
            prefs[EQ_PRESET_NAME] = settings.selectedPresetName
            prefs[EQ_BAND_LEVELS] = settings.bandLevelsMilliBel.joinToString(",")
            prefs[EQ_BASS_BOOST] = settings.bassBoostStrength
            prefs[EQ_VIRTUALIZER] = settings.virtualizerStrength
        }
    }

    suspend fun getEqualizerSettings(): EqualizerSettings =
        equalizerSettingsFlow.first()

    suspend fun saveBubblePosition(x: Float, y: Float) {
        context.settingsDataStore.edit { prefs ->
            prefs[BUBBLE_HAS_POS] = true
            prefs[BUBBLE_X] = x.toString()
            prefs[BUBBLE_Y] = y.toString()
        }
    }

    suspend fun getBubblePosition(): Pair<Float, Float>? {
        val prefs = context.settingsDataStore.data.first()
        if (prefs[BUBBLE_HAS_POS] != true) return null
        val x = prefs[BUBBLE_X]?.toFloatOrNull() ?: return null
        val y = prefs[BUBBLE_Y]?.toFloatOrNull() ?: return null
        return x to y
    }

    suspend fun saveCachedUser(user: User) {
        context.settingsDataStore.edit { prefs ->
            prefs[CACHED_USER_ID] = user.id
            prefs[CACHED_USERNAME] = user.username
            prefs[CACHED_DISPLAY_NAME] = user.displayName
            prefs[CACHED_EMAIL] = user.email
            prefs[CACHED_AVATAR_URL] = user.avatarUrl
            prefs[CACHED_BIO] = user.bio
            prefs[CACHED_IS_PREMIUM] = user.isPremium
            prefs[IS_PREMIUM] = user.isPremium
        }
    }

    suspend fun getCachedUser(): User? {
        val prefs = context.settingsDataStore.data.first()
        val id = prefs[CACHED_USER_ID] ?: return null
        return User(
            id = id,
            username = prefs[CACHED_USERNAME].orEmpty(),
            displayName = prefs[CACHED_DISPLAY_NAME].orEmpty(),
            email = prefs[CACHED_EMAIL].orEmpty(),
            avatarUrl = prefs[CACHED_AVATAR_URL].orEmpty(),
            bio = prefs[CACHED_BIO].orEmpty(),
            isPremium = prefs[CACHED_IS_PREMIUM] ?: prefs[IS_PREMIUM] ?: false,
        )
    }

    suspend fun clearCachedUser() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(CACHED_USER_ID)
            prefs.remove(CACHED_USERNAME)
            prefs.remove(CACHED_DISPLAY_NAME)
            prefs.remove(CACHED_EMAIL)
            prefs.remove(CACHED_AVATAR_URL)
            prefs.remove(CACHED_BIO)
            prefs.remove(CACHED_IS_PREMIUM)
        }
    }

    /** One-shot local chat wipe after product reset; returns true only the first time. */
    suspend fun consumeChatHistoryWipe(): Boolean {
        val prefs = context.settingsDataStore.data.first()
        if (prefs[CHAT_HISTORY_WIPED_V1] == true) return false
        context.settingsDataStore.edit { it[CHAT_HISTORY_WIPED_V1] = true }
        return true
    }
}
