package com.melonet.app.feature.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.melonet.app.data.local.EqualizerSettings
import kotlin.math.roundToInt

/**
 * Process-wide equalizer bound to the current ExoPlayer audio session.
 * Settings are applied whenever the session is recreated.
 */
object EqualizerController {
    @Volatile
    private var equalizer: Equalizer? = null

    @Volatile
    private var bassBoost: BassBoost? = null

    @Volatile
    private var virtualizer: Virtualizer? = null

    @Volatile
    private var settings: EqualizerSettings = EqualizerSettings()

    @Volatile
    private var sessionId: Int = 0

    fun updateSettings(next: EqualizerSettings) {
        settings = next
        applyToHardware()
    }

    fun currentSettings(): EqualizerSettings = settings

    fun onAudioSessionChanged(audioSessionId: Int) {
        if (audioSessionId <= 0) {
            release()
            return
        }
        if (audioSessionId == sessionId && equalizer != null) {
            applyToHardware()
            return
        }
        releaseEffectsOnly()
        sessionId = audioSessionId
        try {
            equalizer = Equalizer(0, audioSessionId).also { it.enabled = settings.enabled }
            bassBoost = runCatching {
                BassBoost(1, audioSessionId).also { it.enabled = settings.bassBoostStrength > 0 }
            }.getOrNull()
            virtualizer = runCatching {
                Virtualizer(2, audioSessionId).also { it.enabled = settings.virtualizerStrength > 0 }
            }.getOrNull()
            applyToHardware()
        } catch (_: Exception) {
            releaseEffectsOnly()
        }
    }

    fun bandCount(): Int = equalizer?.numberOfBands?.toInt() ?: 0

    fun bandLevelRange(): ShortArray =
        equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)

    fun centerFreqHz(band: Int): Int =
        ((equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000)

    fun release() {
        releaseEffectsOnly()
        sessionId = 0
    }

    private fun applyToHardware() {
        val eq = equalizer ?: return
        try {
            eq.enabled = settings.enabled
            val bandCount = eq.numberOfBands.toInt()
            if (settings.usePreset && settings.presetIndex >= 0) {
                val presetCount = eq.numberOfPresets.toInt()
                if (settings.presetIndex < presetCount) {
                    eq.usePreset(settings.presetIndex.toShort())
                }
            } else {
                val levels = settings.bandLevelsMilliBel
                for (i in 0 until bandCount) {
                    val level = levels.getOrNull(i) ?: 0
                    val range = eq.bandLevelRange
                    val clamped = level.coerceIn(range[0].toInt(), range[1].toInt())
                    eq.setBandLevel(i.toShort(), clamped.toShort())
                }
            }
            bassBoost?.let { boost ->
                val strength = settings.bassBoostStrength.coerceIn(0, 1000)
                boost.setStrength(strength.toShort())
                boost.enabled = settings.enabled && strength > 0
            }
            virtualizer?.let { virt ->
                val strength = settings.virtualizerStrength.coerceIn(0, 1000)
                virt.setStrength(strength.toShort())
                virt.enabled = settings.enabled && strength > 0
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseEffectsOnly() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
        try {
            virtualizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    fun builtInPresetNames(): List<String> {
        val eq = equalizer ?: return emptyList()
        return try {
            (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun applyNamedPreset(name: String) {
        val levels = namedPresetLevels(name, bandCount().coerceAtLeast(5))
        updateSettings(
            settings.copy(
                enabled = true,
                usePreset = false,
                presetIndex = -1,
                bandLevelsMilliBel = levels,
            ),
        )
    }

    private fun namedPresetLevels(name: String, bands: Int): List<Int> {
        // Approximate curves in millibels for Bass→Treble style bands.
        val curve = when (name.lowercase()) {
            "bass boost", "bass" -> listOf(800, 500, 100, 0, -100)
            "treble", "treble boost" -> listOf(-100, 0, 100, 500, 800)
            "rock" -> listOf(500, 200, -100, 200, 400)
            "pop" -> listOf(-100, 200, 400, 200, -100)
            "jazz" -> listOf(300, 0, 200, 100, 200)
            "classical" -> listOf(0, 0, 0, 0, 0)
            else -> List(bands) { 0 } // Normal / flat
        }
        return if (curve.size == bands) {
            curve
        } else {
            List(bands) { i ->
                val mapped = (i.toFloat() / (bands - 1).coerceAtLeast(1) * (curve.size - 1)).roundToInt()
                curve[mapped.coerceIn(0, curve.lastIndex)]
            }
        }
    }
}
