package com.melonet.app.feature.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared bridge between [MelonetPlaybackService] and UI (visualizer / equalizer).
 */
object PlaybackAudioBridge {
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _fftMagnitudes = MutableStateFlow(FloatArray(0))
    val fftMagnitudes: StateFlow<FloatArray> = _fftMagnitudes.asStateFlow()

    fun updateAudioSessionId(sessionId: Int) {
        _audioSessionId.value = sessionId.coerceAtLeast(0)
    }

    fun updateFft(magnitudes: FloatArray) {
        _fftMagnitudes.value = magnitudes
    }

    fun clearFft() {
        _fftMagnitudes.value = FloatArray(0)
    }
}
