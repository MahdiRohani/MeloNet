package com.melonet.app.feature.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A real-time "karaoke" audio effect that suppresses center-panned vocals via
 * channel subtraction (L - R). It works on any stereo, 16-bit PCM stream, so it
 * can turn virtually any song into an instrumental-ish backing track.
 *
 * The processor stays in the audio chain at all times (for supported formats)
 * and switches behaviour instantly through [karaokeEnabled] without needing the
 * audio sink to reconfigure.
 */
class KaraokeAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var karaokeEnabled: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            // Unsupported format -> stay inactive (pure passthrough by the sink).
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        // Output format is identical to the input (stereo, 16-bit).
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val output = replaceOutputBuffer(remaining)

        if (!karaokeEnabled) {
            output.put(inputBuffer)
            output.flip()
            return
        }

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        val frames = remaining / 4 // 2 channels * 2 bytes each
        for (i in 0 until frames) {
            val left = inputBuffer.short.toInt()
            val right = inputBuffer.short.toInt()
            // Subtract channels to cancel center-panned vocals, halve to avoid clipping.
            val mixed = ((left - right) / 2).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            output.putShort(mixed)
            output.putShort(mixed)
        }
        // Ensure the whole input is consumed even if it wasn't frame-aligned.
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }
}
