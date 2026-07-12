package com.melonet.app.data.repository

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.mapper.PlaylistMapper
import com.melonet.app.data.mapper.SocialMapper
import com.melonet.app.data.model.Playlist
import com.melonet.app.data.model.PublicUser
import com.melonet.app.data.model.SocialUser
import com.melonet.app.data.model.UserListType
import com.melonet.app.data.remote.SocialApi
import kotlinx.coroutines.withContext

class SocialRepository(
    private val socialApi: SocialApi,
    private val dispatchers: DispatchersProvider,
) {
    suspend fun getUserProfile(userId: Int): Result<PublicUser> = withContext(dispatchers.io) {
        when (val result = safeApiCall { socialApi.getUser(userId) }) {
            is Result.Success -> Result.Success(SocialMapper.toPublicUser(result.data))
            is Result.Error -> result
        }
    }

    suspend fun getUserList(
        userId: Int,
        listType: UserListType,
        page: Int = 1,
        limit: Int = 20,
    ): Result<List<SocialUser>> = withContext(dispatchers.io) {
        when (
            val result = safeApiCall {
                when (listType) {
                    UserListType.FOLLOWERS -> socialApi.getFollowers(userId, page, limit)
                    UserListType.FOLLOWING -> socialApi.getFollowing(userId, page, limit)
                }
            }
        ) {
            is Result.Success -> Result.Success(result.data.map(SocialMapper::toSocialUser))
            is Result.Error -> result
        }
    }

    suspend fun follow(userId: Int): Result<PublicUser> = withContext(dispatchers.io) {
        when (val result = safeApiCall { socialApi.follow(userId) }) {
            is Result.Success -> getUserProfile(userId)
            is Result.Error -> result
        }
    }

    suspend fun unfollow(userId: Int): Result<PublicUser> = withContext(dispatchers.io) {
        when (val result = safeApiCall { socialApi.unfollow(userId) }) {
            is Result.Success -> getUserProfile(userId)
            is Result.Error -> result
        }
    }

    suspend fun getUserPlaylists(
        userId: Int,
        page: Int = 1,
        limit: Int = 20,
    ): Result<List<Playlist>> = withContext(dispatchers.io) {
        when (val result = safeApiCall { socialApi.getUserPlaylists(userId, page, limit) }) {
            is Result.Success -> Result.Success(result.data.map(PlaylistMapper::toModel))
            is Result.Error -> result
        }
    }
}
