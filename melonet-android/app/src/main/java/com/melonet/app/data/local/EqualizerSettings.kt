package com.melonet.app.data.local

/**
 * Persisted equalizer configuration.
 * [bandLevelsMilliBel] are relative gains for each hardware band.
 */
data class EqualizerSettings(
    val enabled: Boolean = true,
    val usePreset: Boolean = false,
    val presetIndex: Int = -1,
    val bandLevelsMilliBel: List<Int> = emptyList(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val selectedPresetName: String = "Normal",
)
