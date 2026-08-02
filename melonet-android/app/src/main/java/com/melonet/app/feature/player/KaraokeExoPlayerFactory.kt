package com.melonet.app.feature.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Builds ExoPlayers for karaoke take playback.
 * Instrumental streams go through `/api/stream` → CDN (HTTP→HTTPS 302), so the
 * data source must allow cross-protocol redirects — same as [MelonetPlaybackService].
 */
object KaraokeExoPlayerFactory {

    fun createInstrumentalPlayer(
        context: Context,
        handleAudioFocus: Boolean = false,
    ): Pair<ExoPlayer, KaraokeAudioProcessor> {
        val karaokeProcessor = KaraokeAudioProcessor().apply { karaokeEnabled = true }
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf<AudioProcessor>(karaokeProcessor))
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }
        val player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(redirectAwareMediaSourceFactory(context))
            .setAudioAttributes(mediaAudioAttributes(), handleAudioFocus)
            .build()
        // Backing under the mic take so the user's voice stays clear (~−8 dB vs vocal).
        player.volume = TAKE_INSTRUMENTAL_VOLUME
        return player to karaokeProcessor
    }

    fun createVocalPlayer(
        context: Context,
        handleAudioFocus: Boolean = false,
    ): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(redirectAwareMediaSourceFactory(context))
            .setAudioAttributes(mediaAudioAttributes(), handleAudioFocus)
            .build()
            .also { it.volume = TAKE_VOCAL_VOLUME }
    }

    private fun mediaAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

    private fun redirectAwareMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(60_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("MeloNet/Android")
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        return DefaultMediaSourceFactory(dataSourceFactory)
    }

    private const val TAKE_INSTRUMENTAL_VOLUME = 0.18f
    private const val TAKE_VOCAL_VOLUME = 1f
}
