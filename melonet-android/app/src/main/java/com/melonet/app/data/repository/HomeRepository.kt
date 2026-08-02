package com.melonet.app.data.repository

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.mapper.HomeMapper
import com.melonet.app.data.model.HomeFeed
import com.melonet.app.data.remote.HomeApi
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

class HomeRepository(
    private val homeApi: HomeApi,
    private val dispatchers: DispatchersProvider,
) {
    private data class CachedFeed(val feed: HomeFeed, val cachedAtMs: Long)

    private val memoryCache = AtomicReference<CachedFeed?>(null)

    suspend fun getHomeFeed(forceRefresh: Boolean = false): Result<HomeFeed> = withContext(dispatchers.io) {
        if (!forceRefresh) {
            memoryCache.get()?.let { cached ->
                if (System.currentTimeMillis() - cached.cachedAtMs < MEMORY_TTL_MS) {
                    return@withContext Result.Success(cached.feed)
                }
            }
        }
        when (val result = safeApiCall { homeApi.getHomeFeed() }) {
            is Result.Success -> {
                val feed = HomeMapper.toModel(result.data)
                // Incomplete soft-deadline responses often omit artists; don't pin those.
                if (feed.artistRows.any { it.items.isNotEmpty() } || feed.isEmpty) {
                    memoryCache.set(CachedFeed(feed, System.currentTimeMillis()))
                }
                Result.Success(feed)
            }
            is Result.Error -> {
                // Prefer stale memory over a hard failure on first paint / karaoke.
                memoryCache.get()?.let { return@withContext Result.Success(it.feed) }
                result
            }
        }
    }

    fun peekCachedFeed(): HomeFeed? = memoryCache.get()?.feed

    companion object {
        private const val MEMORY_TTL_MS = 5 * 60_000L
    }
}
