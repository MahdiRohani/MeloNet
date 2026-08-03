package com.melonet.app.feature.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Taps PCM from the ExoPlayer audio chain and publishes equalizer bar magnitudes.
 * More reliable than [android.media.audiofx.Visualizer] (often silent on Samsung
 * without mic permission / with other audio effects attached).
 */
class VisualizerAudioProcessor(
    private val barCount: Int = 48,
) : BaseAudioProcessor() {

    private val smoothed = FloatArray(barCount)
    private var peakEma = 0.25f
    private var lastEmitNs = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount !in 1..8) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val bytesPerFrame = 2 * channels
        val frames = remaining / bytesPerFrame
        if (frames > 0) {
            analyze(inputBuffer, channels, frames)
            maybeEmit()
        }

        val output = replaceOutputBuffer(remaining)
        output.put(inputBuffer)
        output.flip()
    }

    override fun onFlush() {
        smoothed.fill(0f)
        peakEma = 0.25f
        PlaybackAudioBridge.clearFft()
    }

    override fun onReset() {
        onFlush()
    }

    private fun analyze(buffer: ByteBuffer, channels: Int, frames: Int) {
        val order = buffer.order()
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val startPos = buffer.position()
        var framePeak = 1e-4f
        val raw = FloatArray(barCount)

        for (i in 0 until barCount) {
            val f0 = (i * frames) / barCount
            val f1 = ((i + 1) * frames) / barCount
            var localPeak = 0
            var sumSq = 0.0
            var count = 0
            for (f in f0 until f1) {
                var frameAbs = 0
                val base = startPos + f * channels * 2
                for (c in 0 until channels) {
                    val sample = abs(buffer.getShort(base + c * 2).toInt())
                    if (sample > frameAbs) frameAbs = sample
                }
                if (frameAbs > localPeak) localPeak = frameAbs
                val n = frameAbs / 32768.0
                sumSq += n * n
                count++
            }
            val peak = localPeak / 32768f
            val rms = if (count > 0) sqrt(sumSq / count).toFloat() else 0f
            val amp = peak * 0.7f + rms * 0.3f
            raw[i] = amp
            if (amp > framePeak) framePeak = amp
        }

        buffer.order(order)
        peakEma = max(framePeak, peakEma * 0.90f + framePeak * 0.10f)
        val denom = peakEma.coerceAtLeast(0.08f)

        for (i in smoothed.indices) {
            val shaped = sqrt((raw[i] / denom).coerceIn(0f, 1f))
            smoothed[i] = if (shaped > smoothed[i]) {
                smoothed[i] * 0.30f + shaped * 0.70f
            } else {
                smoothed[i] * 0.72f + shaped * 0.28f
            }
        }
    }

    private fun maybeEmit() {
        val now = System.nanoTime()
        if (now - lastEmitNs < EMIT_INTERVAL_NS) return
        lastEmitNs = now
        PlaybackAudioBridge.updateFft(smoothed.copyOf())
    }

    companion object {
        private const val EMIT_INTERVAL_NS = 33_000_000L // ~30 Hz
    }
}
