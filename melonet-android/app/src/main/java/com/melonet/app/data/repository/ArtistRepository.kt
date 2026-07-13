package com.melonet.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.mapper.ArtistMapper
import com.melonet.app.data.model.Artist
import com.melonet.app.data.model.Song
import com.melonet.app.data.paging.ArtistSongsPagingSource
import com.melonet.app.data.remote.ArtistApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ArtistRepository(
    private val artistApi: ArtistApi,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun getArtists(region: String?, page: Int = 1, limit: Int = 20): Result<List<Artist>> =
        withContext(dispatchers.io) {
            when (val result = safeApiCall { artistApi.getArtists(region, page, limit) }) {
                is Result.Success -> Result.Success(result.data.map(ArtistMapper::toModel))
                is Result.Error -> result
            }
        }

    suspend fun getArtist(artistId: Int): Result<Artist> = withContext(dispatchers.io) {
        when (val result = safeApiCall { artistApi.getArtist(artistId) }) {
            is Result.Success -> Result.Success(ArtistMapper.toModel(result.data))
            is Result.Error -> result
        }
    }

    fun artistSongs(artistId: Int, sort: String?): Flow<PagingData<Song>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            ArtistSongsPagingSource(artistApi = artistApi, artistId = artistId, sort = sort)
        },
    ).flow

    suspend fun follow(artistId: Int): Result<Artist> = withContext(dispatchers.io) {
        when (val result = safeApiCall { artistApi.follow(artistId) }) {
            is Result.Success -> Result.Success(ArtistMapper.toModel(result.data))
            is Result.Error -> result
        }
    }

    suspend fun unfollow(artistId: Int): Result<Artist> = withContext(dispatchers.io) {
        when (val result = safeApiCall { artistApi.unfollow(artistId) }) {
            is Result.Success -> Result.Success(ArtistMapper.toModel(result.data))
            is Result.Error -> result
        }
    }

    suspend fun followedArtists(page: Int = 1, limit: Int = 50): Result<List<Artist>> =
        withContext(dispatchers.io) {
            when (val result = safeApiCall { artistApi.getFollowedArtists(page, limit) }) {
                is Result.Success -> Result.Success(result.data.map(ArtistMapper::toModel))
                is Result.Error -> result
            }
        }
}
