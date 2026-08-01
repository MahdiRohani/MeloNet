package com.melonet.app.feature.player

import android.media.audiofx.Visualizer
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Captures FFT magnitudes from the ExoPlayer audio session for the player visualizer.
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
                    ) = Unit

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (fft == null || fft.size < 4) return
                        mapFftToBars(fft, output)
                        onMagnitudes(output.copyOf())
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true,
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

    private fun mapFftToBars(fft: ByteArray, out: FloatArray) {
        val n = fft.size / 2
        val usable = (n - 1).coerceAtLeast(1)
        for (i in out.indices) {
            val start = 1 + (i * usable) / out.size
            val end = 1 + ((i + 1) * usable) / out.size
            var maxMag = 0f
            for (k in start until end.coerceAtMost(n)) {
                val re = fft[k * 2].toInt()
                val im = fft[k * 2 + 1].toInt()
                val mag = sqrt((re * re + im * im).toFloat())
                if (mag > maxMag) maxMag = mag
            }
            // Convert to a pleasant 0..1 display range.
            val db = if (maxMag > 1f) 20f * log10(maxMag) else 0f
            out[i] = ((db - 5f) / 45f).coerceIn(0.02f, 1f)
        }
    }
}
