package com.melonet.app.feature.player

import android.content.Context
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class MelonetPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val karaokeProcessor = KaraokeAudioProcessor()
    private val visualizerProcessor = VisualizerAudioProcessor()

    override fun onCreate() {
        super.onCreate()

        val cache = PlayerCache.get(this)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(60_000)
            .setAllowCrossProtocolRedirects(true)
        val upstreamFactory = DefaultDataSource.Factory(
            this,
            httpDataSourceFactory,
        )
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(
                        arrayOf<AudioProcessor>(
                            karaokeProcessor,
                            visualizerProcessor,
                        ),
                    )
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .build()
        player.volume = 1f

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                PlaybackAudioBridge.updateAudioSessionId(audioSessionId)
                EqualizerController.onAudioSessionChanged(audioSessionId)
            }
        })
        // Session id may already be assigned before the listener is attached.
        val initialSession = player.audioSessionId
        if (initialSession != C.AUDIO_SESSION_ID_UNSET) {
            PlaybackAudioBridge.updateAudioSessionId(initialSession)
            EqualizerController.onAudioSessionChanged(initialSession)
        }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MelonetSessionCallback())
            .build()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    private inner class MelonetSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(COMMAND_SET_KARAOKE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                .build()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<androidx.media3.common.MediaItem>,
        ): ListenableFuture<List<androidx.media3.common.MediaItem>> {
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    val uri = item.requestMetadata.mediaUri
                    if (uri != null) {
                        item.buildUpon().setUri(uri).build()
                    } else {
                        item
                    }
                }
            }
            return Futures.immediateFuture(resolved)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<androidx.media3.common.MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return Futures.transform(
                onAddMediaItems(mediaSession, controller, mediaItems),
                { resolved ->
                    MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs)
                },
                MoreExecutors.directExecutor(),
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == COMMAND_SET_KARAOKE) {
                val enabled = args.getBoolean(KEY_KARAOKE_ENABLED, false)
                karaokeProcessor.karaokeEnabled = enabled
                // Keep backing quieter so the singer (mic) stays readable live and on takes.
                player.volume = if (enabled) KARAOKE_BACKING_VOLUME else 1f
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        PlaybackAudioBridge.clearFft()
        EqualizerController.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val COMMAND_SET_KARAOKE = "com.melonet.app.command.SET_KARAOKE"
        const val KEY_KARAOKE_ENABLED = "karaoke_enabled"
        /** Backing-track level while karaoke (instrumental) mode is on. */
        const val KARAOKE_BACKING_VOLUME = 0.22f
    }
}
