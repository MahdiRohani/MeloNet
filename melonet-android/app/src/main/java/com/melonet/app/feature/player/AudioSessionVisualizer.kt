package com.melonet.app.feature.player

import android.media.audiofx.Visualizer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Captures FFT (+ light waveform energy) from the ExoPlayer audio session.
 * Bars track song loudness: quiet passages stay low, peaks rise clearly.
 */
class AudioSessionVisualizer(
    private val barCount: Int = 48,
    private val onMagnitudes: (FloatArray) -> Unit,
) {
    private var visualizer: Visualizer? = null
    private val output = FloatArray(barCount)
    private val smoothed = FloatArray(barCount)
    private var peakEma = 24f
    @Volatile
    private var waveEnergy = 0f

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
                        var sumSq = 0.0
                        for (b in waveform) {
                            val centered = (b.toInt() and 0xFF) - 128
                            sumSq += centered * centered
                        }
                        waveEnergy = sqrt(sumSq / waveform.size).toFloat() / 128f
                    }

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
                true,
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
        peakEma = 24f
        waveEnergy = 0f
        smoothed.fill(0f)
        onMagnitudes(FloatArray(barCount))
    }

    private fun mapFftToBars(fft: ByteArray, out: FloatArray) {
        val n = fft.size / 2
        val usable = (n - 1).coerceAtLeast(1)
        val raw = FloatArray(out.size)
        var framePeak = 1e-3f

        for (i in raw.indices) {
            val start = 1 + (i * usable) / out.size
            val end = 1 + ((i + 1) * usable) / out.size
            var sumSq = 0.0
            var count = 0
            for (k in start until end.coerceAtMost(n)) {
                val re = fft[k * 2].toInt().toFloat()
                val im = fft[k * 2 + 1].toInt().toFloat()
                sumSq += re * re + im * im
                count++
            }
            val rms = if (count > 0) sqrt(sumSq / count).toFloat() else 0f
            raw[i] = rms
            if (rms > framePeak) framePeak = rms
        }

        // Adaptive ceiling so bars use the full 0..1 range with real dynamics.
        peakEma = max(framePeak, peakEma * 0.92f + framePeak * 0.08f)
        val denom = peakEma.coerceAtLeast(12f)

        // Overall waveform energy scales the whole equalizer with song volume.
        val volume = (waveEnergy * 1.35f).coerceIn(0.15f, 1f)

        for (i in out.indices) {
            val normalized = (raw[i] / denom).coerceIn(0f, 1f)
            // Mild curve keeps quiet notes visible but still volume-linked.
            val shaped = sqrt(normalized) * volume
            smoothed[i] = if (shaped > smoothed[i]) {
                smoothed[i] * 0.35f + shaped * 0.65f
            } else {
                smoothed[i] * 0.78f + shaped * 0.22f
            }
            out[i] = smoothed[i].coerceIn(0.03f, 1f)
        }
    }
}
