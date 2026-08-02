package com.melonet.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.local.LikedSongDao
import com.melonet.app.data.local.PlayHistoryDao
import com.melonet.app.data.local.toLikedEntity
import com.melonet.app.data.local.toPlayHistoryEntity
import com.melonet.app.data.local.LocalPlaylistDao
import com.melonet.app.data.local.LocalPlaylistSongEntity
import com.melonet.app.data.mapper.PlaylistMapper
import com.melonet.app.data.model.Playlist
import com.melonet.app.data.model.PlaylistScope
import com.melonet.app.data.model.Song
import com.melonet.app.data.paging.LikedSongsPagingSource
import com.melonet.app.data.paging.PlaylistSongsPagingSource
import com.melonet.app.data.paging.PlaylistsPagingSource
import com.melonet.app.data.paging.RecentSongsPagingSource
import com.melonet.app.data.remote.LibraryApi
import com.melonet.app.data.remote.PlaylistApi
import com.melonet.app.data.remote.dto.AddPlaylistSongRequestDto
import com.melonet.app.data.remote.dto.CreatePlaylistRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val playlistApi: PlaylistApi,
    private val localPlaylistDao: LocalPlaylistDao,
    private val dispatchers: DispatchersProvider,
) {
    fun playlists(scope: PlaylistScope): Flow<PagingData<Playlist>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            PlaylistsPagingSource(
                playlistApi = playlistApi,
                scope = scope.apiValue,
                localPlaylistDao = localPlaylistDao.takeIf { scope == PlaylistScope.MINE },
            )
        },
    ).flow

    suspend fun getPlaylist(id: Int): Result<Playlist> = withContext(dispatchers.io) {
        when (val result = safeApiCall { playlistApi.getPlaylist(id) }) {
            is Result.Success -> Result.Success(enrichPlaylistWithLocal(PlaylistMapper.toModel(result.data)))
            is Result.Error -> result
        }
    }

    suspend fun getUserPlaylists(): Result<List<Playlist>> = withContext(dispatchers.io) {
        when (
            val result = safeApiCall {
                playlistApi.getPlaylists(PlaylistScope.MINE.apiValue, page = 1, limit = 50)
            }
        ) {
            is Result.Success -> {
                val playlists = result.data.map(PlaylistMapper::toModel)
                Result.Success(enrichPlaylistsWithLocal(playlists))
            }
            is Result.Error -> result
        }
    }

    suspend fun createPlaylist(title: String): Result<Playlist> = withContext(dispatchers.io) {
        when (
            val result = safeApiCall {
                playlistApi.createPlaylist(CreatePlaylistRequestDto(title = title))
            }
        ) {
            is Result.Success -> Result.Success(PlaylistMapper.toModel(result.data))
            is Result.Error -> result
        }
    }

    suspend fun deletePlaylist(id: Int): Result<Unit> = withContext(dispatchers.io) {
        when (val result = safeApiCall { playlistApi.deletePlaylist(id) }) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> result
        }
    }

    fun playlistSongs(playlistId: Int): Flow<PagingData<Song>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            PlaylistSongsPagingSource(playlistApi, playlistId, localPlaylistDao)
        },
    ).flow

    suspend fun getLocalPlaylistSongs(playlistId: Int): List<Song> = withContext(dispatchers.io) {
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
    }

    suspend fun addLocalSongToPlaylist(playlistId: Int, song: Song): Result<Unit> =
        withContext(dispatchers.io) {
            val isLocal = song.id.startsWith("local_") || song.category == "local"
            if (!isLocal) {
                safeApiCall {
                    playlistApi.addPlaylistSong(
                        playlistId,
                        AddPlaylistSongRequestDto(songId = song.id),
                    )
                }
            }

            if (!localPlaylistDao.exists(playlistId, song.id)) {
                localPlaylistDao.insert(
                    LocalPlaylistSongEntity(
                        playlistId = playlistId,
                        songId = song.id,
                        title = song.title,
                        artistName = song.artistName,
                        coverUrl = song.coverUrl,
                        audioUrl = song.audioUrl,
                        durationSec = song.durationSec,
                        isLocal = isLocal,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
            }
            Result.Success(Unit)
        }

    /** Push Room-only cloud songs up to the API so counts/covers match after older installs. */
    suspend fun syncLocalSongsToServer(playlistId: Int) = withContext(dispatchers.io) {
        val local = localPlaylistDao.getSongsForPlaylist(playlistId)
        for (entity in local) {
            if (entity.isLocal || entity.songId.startsWith("local_")) continue
            safeApiCall {
                playlistApi.addPlaylistSong(
                    playlistId,
                    AddPlaylistSongRequestDto(songId = entity.songId),
                )
            }
        }
    }

    suspend fun removeLocalSongFromPlaylist(playlistId: Int, songId: String): Result<Unit> =
        withContext(dispatchers.io) {
            localPlaylistDao.delete(playlistId, songId)
            Result.Success(Unit)
        }

    suspend fun removeSongFromPlaylist(playlistId: Int, songId: String): Result<Unit> =
        withContext(dispatchers.io) {
            if (songId.startsWith("local_")) {
                return@withContext removeLocalSongFromPlaylist(playlistId, songId)
            }
            when (val result = safeApiCall { playlistApi.removePlaylistSong(playlistId, songId) }) {
                is Result.Success -> {
                    localPlaylistDao.delete(playlistId, songId)
                    Result.Success(Unit)
                }
                is Result.Error -> result
            }
        }

    private suspend fun enrichPlaylistWithLocal(playlist: Playlist): Playlist {
        return enrichPlaylistsWithLocal(listOf(playlist)).first()
    }

    private suspend fun enrichPlaylistsWithLocal(playlists: List<Playlist>): List<Playlist> {
        if (playlists.isEmpty()) return playlists
        val localSongs = localPlaylistDao.getSongsForPlaylists(playlists.map { it.id })
        val byPlaylist = localSongs.groupBy { it.playlistId }
        return playlists.map { playlist ->
            val local = byPlaylist[playlist.id].orEmpty()
            if (local.isEmpty()) return@map playlist
            val localCovers = local.map { it.coverUrl }.filter { it.isNotBlank() }.distinct().take(4)
            val mosaic = pickMosaicCovers(playlist.coverUrls, localCovers)
            playlist.copy(
                songCount = maxOf(playlist.songCount, local.size),
                coverUrls = mosaic,
                coverUrl = playlist.coverUrl.ifBlank { mosaic.firstOrNull().orEmpty() },
            )
        }
    }

    private fun pickMosaicCovers(apiCovers: List<String>, localCovers: List<String>): List<String> {
        val api = apiCovers.filter { it.isNotBlank() }.distinct().take(4)
        val local = localCovers.filter { it.isNotBlank() }.distinct().take(4)
        // Prefer the richer mosaic (up to 4 song arts), not a single cover_url fallback.
        return when {
            local.size >= 2 && local.size >= api.size -> local
            api.size >= 2 -> api
            local.isNotEmpty() -> local
            else -> api
        }
    }
}

class LibraryRepository(
    private val libraryApi: LibraryApi,
    private val likedSongDao: LikedSongDao,
    private val playHistoryDao: PlayHistoryDao,
    private val dispatchers: DispatchersProvider,
) {
    fun likedSongs(): Flow<PagingData<Song>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            LikedSongsPagingSource(libraryApi) { songs ->
                likedSongDao.insertAll(songs.map { it.toLikedEntity() })
            }
        },
    ).flow

    fun recentSongs(): Flow<PagingData<Song>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            RecentSongsPagingSource(libraryApi) { songs ->
                playHistoryDao.insertAll(songs.map { it.toPlayHistoryEntity() })
            }
        },
    ).flow

    fun observeIsLiked(songId: String): Flow<Boolean> = likedSongDao.observeIsLiked(songId)

    suspend fun likeSong(song: Song): Result<Unit> = withContext(dispatchers.io) {
        when (val result = safeApiCall { libraryApi.likeSong(song.id) }) {
            is Result.Success -> {
                likedSongDao.insert(song.toLikedEntity())
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    suspend fun unlikeSong(songId: String): Result<Unit> = withContext(dispatchers.io) {
        when (val result = safeApiCall { libraryApi.unlikeSong(songId) }) {
            is Result.Success -> {
                likedSongDao.delete(songId)
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    suspend fun dismissRecentSong(songId: String) = withContext(dispatchers.io) {
        playHistoryDao.delete(songId)
    }
}
