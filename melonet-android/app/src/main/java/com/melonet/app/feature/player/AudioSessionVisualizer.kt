package com.melonet.app.feature.player

import android.media.audiofx.Visualizer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Captures waveform amplitudes from the ExoPlayer audio session for the player visualizer.
 * Bars reflect local temporal energy plus overall RMS so motion is predominantly vertical.
 */
class AudioSessionVisualizer(
    private val barCount: Int = 48,
    private val onMagnitudes: (FloatArray) -> Unit,
) {
    private var visualizer: Visualizer? = null
    private val output = FloatArray(barCount)

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId <= 0) return
        try {
            val viz = Visualizer(audioSessionId)
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = min(1024, max(range[0], min(range[1], 512)))
            viz.captureSize = captureSize
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (waveform == null || waveform.isEmpty()) return
                        mapWaveformToBars(waveform, output)
                        onMagnitudes(output.copyOf())
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) = Unit
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false,
            )
            viz.enabled = true
            visualizer = viz
        } catch (_: Exception) {
            release()
        }
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        }
        visualizer = null
        onMagnitudes(FloatArray(barCount))
    }

    /**
     * Each bar = local waveform amplitude for a time slice, lifted by global RMS
     * so louder playback raises the whole set of bars rather than shifting peaks sideways.
     */
    private fun mapWaveformToBars(waveform: ByteArray, out: FloatArray) {
        val n = waveform.size
        var sumSq = 0.0
        for (b in waveform) {
            val centered = (b.toInt() and 0xFF) - 128
            sumSq += centered * centered.toDouble()
        }
        val rms = (sqrt(sumSq / n) / 128.0).toFloat().coerceIn(0f, 1f)

        for (i in out.indices) {
            val start = (i * n) / out.size
            val end = ((i + 1) * n) / out.size
            val count = (end - start).coerceAtLeast(1)
            var localSumSq = 0.0
            var localPeak = 0f
            for (k in start until end) {
                val centered = ((waveform[k].toInt() and 0xFF) - 128).toFloat()
                localSumSq += centered * centered
                val a = abs(centered)
                if (a > localPeak) localPeak = a
            }
            val localRms = (sqrt(localSumSq / count) / 128.0).toFloat()
            val localAmp = localRms * 0.6f + (localPeak / 128f) * 0.4f
            // Mix local detail with global loudness so volume changes read as up/down.
            out[i] = (localAmp * 0.55f + rms * 0.45f).coerceIn(0.03f, 1f)
        }
    }
}
