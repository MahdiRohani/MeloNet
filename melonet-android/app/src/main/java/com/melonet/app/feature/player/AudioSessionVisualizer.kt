package com.melonet.app.feature.player

import android.media.audiofx.Visualizer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Captures waveform amplitudes from the ExoPlayer audio session for the player visualizer.
 * Bars emphasize local temporal contrast (with light auto-gain) so motion reads clearly vertical.
 */
class AudioSessionVisualizer(
    private val barCount: Int = 40,
    private val onMagnitudes: (FloatArray) -> Unit,
) {
    private var visualizer: Visualizer? = null
    private val output = FloatArray(barCount)
    private val smoothed = FloatArray(barCount)
    private var gain = 1f

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
        gain = 1f
        smoothed.fill(0f)
        onMagnitudes(FloatArray(barCount))
    }

    private fun mapWaveformToBars(waveform: ByteArray, out: FloatArray) {
        val n = waveform.size
        var framePeak = 0.001f
        val raw = FloatArray(out.size)

        for (i in raw.indices) {
            val start = (i * n) / raw.size
            val end = ((i + 1) * n) / raw.size
            var localPeak = 0f
            var localSumSq = 0.0
            val count = (end - start).coerceAtLeast(1)
            for (k in start until end) {
                val centered = ((waveform[k].toInt() and 0xFF) - 128).toFloat()
                val a = abs(centered)
                if (a > localPeak) localPeak = a
                localSumSq += centered * centered
            }
            val localRms = sqrt(localSumSq / count).toFloat()
            // Prefer peak detail so neighboring bars differ clearly.
            val amp = (localPeak * 0.75f + localRms * 0.25f) / 128f
            raw[i] = amp
            if (amp > framePeak) framePeak = amp
        }

        // Soft auto-gain: keep relative bar motion without stretching waves to full height.
        val targetGain = (0.55f / framePeak).coerceIn(1f, 2.2f)
        gain = gain * 0.85f + targetGain * 0.15f

        for (i in out.indices) {
            val boosted = (raw[i] * gain * 0.72f).coerceIn(0f, 1f)
            // Fast attack / slower release so bars feel lively, not a fixed pulse.
            smoothed[i] = if (boosted > smoothed[i]) {
                smoothed[i] * 0.35f + boosted * 0.65f
            } else {
                smoothed[i] * 0.72f + boosted * 0.28f
            }
            out[i] = smoothed[i].coerceIn(0.02f, 1f)
        }
    }
}
