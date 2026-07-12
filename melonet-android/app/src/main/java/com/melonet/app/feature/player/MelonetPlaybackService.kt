package com.melonet.app.feature.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MelonetPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: Player
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var crossfadeJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        val cache = PlayerCache.get(this)
        val upstreamFactory = DefaultDataSource.Factory(
            this,
            DefaultHttpDataSource.Factory(),
        )
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                ) {
                    crossfadeIn()
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    private fun crossfadeIn() {
        crossfadeJob?.cancel()
        player.volume = 0f
        crossfadeJob = serviceScope.launch {
            val steps = CROSSFADE_STEPS
            val stepDelay = CROSSFADE_MS / steps
            for (step in 1..steps) {
                player.volume = step / steps.toFloat()
                delay(stepDelay)
            }
            player.volume = 1f
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        crossfadeJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        private const val CROSSFADE_MS = 1500L
        private const val CROSSFADE_STEPS = 15
    }
}
