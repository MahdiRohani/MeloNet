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
import com.melonet.app.data.remote.dto.CreatePlaylistRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val playlistApi: PlaylistApi,
    private val dispatchers: DispatchersProvider,
) {
    fun playlists(scope: PlaylistScope): Flow<PagingData<Playlist>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            PlaylistsPagingSource(playlistApi, scope.apiValue)
        },
    ).flow

    suspend fun getPlaylist(id: Int): Result<Playlist> = withContext(dispatchers.io) {
        when (val result = safeApiCall { playlistApi.getPlaylist(id) }) {
            is Result.Success -> Result.Success(PlaylistMapper.toModel(result.data))
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
            PlaylistSongsPagingSource(playlistApi, playlistId)
        },
    ).flow
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
