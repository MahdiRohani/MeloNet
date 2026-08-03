package com.melonet.app.feature.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

/**
 * Shared bridge between [MelonetPlaybackService] and UI (visualizer / equalizer).
 * FFT publishes via a double-buffer so emits stay allocation-free.
 */
object PlaybackAudioBridge {
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val fftA = FloatArray(48)
    private val fftB = FloatArray(48)
    private var publishA = true
    private val _fftMagnitudes = MutableStateFlow(FloatArray(0))
    val fftMagnitudes: StateFlow<FloatArray> = _fftMagnitudes.asStateFlow()

    fun updateAudioSessionId(sessionId: Int) {
        _audioSessionId.value = sessionId.coerceAtLeast(0)
    }

    fun updateFft(magnitudes: FloatArray) {
        val dest = if (publishA) fftA else fftB
        val n = min(magnitudes.size, dest.size)
        System.arraycopy(magnitudes, 0, dest, 0, n)
        if (n < dest.size) dest.fill(0f, n, dest.size)
        publishA = !publishA
        _fftMagnitudes.value = dest
    }

    fun clearFft() {
        fftA.fill(0f)
        fftB.fill(0f)
        _fftMagnitudes.value = FloatArray(0)
    }
}
