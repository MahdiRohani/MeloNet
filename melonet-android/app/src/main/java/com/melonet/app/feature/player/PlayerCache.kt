package com.melonet.app.feature.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object PlayerCache {
    private const val CACHE_SIZE_BYTES = 100L * 1024 * 1024

    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        return instance ?: synchronized(this) {
            instance ?: run {
                val cacheDir = File(context.cacheDir, "melonet_media_cache")
                SimpleCache(
                    cacheDir,
                    LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES),
                    StandaloneDatabaseProvider(context),
                )
            }.also { instance = it }
        }
    }
}
