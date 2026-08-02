package com.melonet.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.melonet.app.data.local.LocalPlaylistDao
import com.melonet.app.data.mapper.PlaylistMapper
import com.melonet.app.data.mapper.SongMapper
import com.melonet.app.data.model.Playlist
import com.melonet.app.data.model.Song
import com.melonet.app.data.remote.LibraryApi
import com.melonet.app.data.remote.PlaylistApi
import com.melonet.app.data.remote.dto.AddPlaylistSongRequestDto
import java.io.IOException

class PlaylistSongsPagingSource(
    private val playlistApi: PlaylistApi,
    private val playlistId: Int,
    private val localPlaylistDao: LocalPlaylistDao,
) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val page = params.key ?: 1
        return try {
            val response = playlistApi.getPlaylistSongs(
                id = playlistId,
                page = page,
                limit = params.loadSize,
            )
            response.error?.let { return LoadResult.Error(IOException(it.message)) }
            val apiItems = response.data?.map(SongMapper::toModel).orEmpty()
            val localItems = if (page == 1) {
                localPlaylistDao.getSongsForPlaylist(playlistId).map { entity ->
                    Song(
                        id = entity.songId,
                        title = entity.title,
                        artistName = entity.artistName,
                        coverUrl = entity.coverUrl,
                        audioUrl = entity.audioUrl,
                        category = if (entity.isLocal) "local" else "app",
                        lyrics = "",
                        durationSec = entity.durationSec,
                    )
                }
            } else {
                emptyList()
            }
            val items = localItems + apiItems.filter { apiSong ->
                localItems.none { it.id == apiSong.id }
            }
            val hasMore = response.meta?.hasMore == true
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (hasMore) page + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

class LikedSongsPagingSource(
    private val libraryApi: LibraryApi,
    private val onCache: suspend (List<Song>) -> Unit,
) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val page = params.key ?: 1
        return try {
            val response = libraryApi.getLikedSongs(page = page, limit = params.loadSize)
            response.error?.let { return LoadResult.Error(IOException(it.message)) }
            val items = response.data?.map(SongMapper::toModel).orEmpty()
            onCache(items)
            val hasMore = response.meta?.hasMore == true
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (hasMore) page + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

class RecentSongsPagingSource(
    private val libraryApi: LibraryApi,
    private val onCache: suspend (List<Song>) -> Unit,
) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val page = params.key ?: 1
        return try {
            val response = libraryApi.getRecentSongs(page = page, limit = params.loadSize)
            response.error?.let { return LoadResult.Error(IOException(it.message)) }
            val items = response.data?.map(SongMapper::toModel).orEmpty()
            onCache(items)
            val hasMore = response.meta?.hasMore == true
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (hasMore) page + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

class PlaylistsPagingSource(
    private val playlistApi: PlaylistApi,
    private val scope: String,
    private val localPlaylistDao: LocalPlaylistDao? = null,
) : PagingSource<Int, Playlist>() {

    override fun getRefreshKey(state: PagingState<Int, Playlist>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Playlist> {
        val page = params.key ?: 1
        return try {
            val response = playlistApi.getPlaylists(
                scope = scope,
                page = page,
                limit = params.loadSize,
            )
            response.error?.let { return LoadResult.Error(IOException(it.message)) }
            var items = response.data?.map(PlaylistMapper::toModel).orEmpty()
            val dao = localPlaylistDao
            if (dao != null && items.isNotEmpty()) {
                // Backfill songs that were saved only in Room on older builds.
                if (page == 1) {
                    items.forEach { playlist ->
                        val local = dao.getSongsForPlaylist(playlist.id)
                        for (entity in local) {
                            if (entity.isLocal || entity.songId.startsWith("local_")) continue
                            runCatching {
                                playlistApi.addPlaylistSong(
                                    playlist.id,
                                    AddPlaylistSongRequestDto(songId = entity.songId),
                                )
                            }
                        }
                    }
                    // Re-fetch so server song_count / cover_urls reflect the sync.
                    val refreshed = playlistApi.getPlaylists(
                        scope = scope,
                        page = page,
                        limit = params.loadSize,
                    )
                    if (refreshed.error == null) {
                        items = refreshed.data?.map(PlaylistMapper::toModel).orEmpty()
                    }
                }
                val localSongs = dao.getSongsForPlaylists(items.map { it.id })
                val byPlaylist = localSongs.groupBy { it.playlistId }
                items = items.map { playlist ->
                    val local = byPlaylist[playlist.id].orEmpty()
                    if (local.isEmpty()) return@map playlist
                    val localCovers = local.map { it.coverUrl }.filter { it.isNotBlank() }.distinct().take(4)
                    val apiCovers = playlist.coverUrls.filter { it.isNotBlank() }.distinct().take(4)
                    val mosaic = when {
                        localCovers.size >= 2 && localCovers.size >= apiCovers.size -> localCovers
                        apiCovers.size >= 2 -> apiCovers
                        localCovers.isNotEmpty() -> localCovers
                        else -> apiCovers
                    }
                    playlist.copy(
                        songCount = maxOf(playlist.songCount, local.size),
                        coverUrls = mosaic,
                        coverUrl = playlist.coverUrl.ifBlank { mosaic.firstOrNull().orEmpty() },
                    )
                }
            }
            val hasMore = response.meta?.hasMore == true
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (hasMore) page + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
