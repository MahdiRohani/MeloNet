package com.melonet.app.data.repository

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.data.mapper.HomeMapper
import com.melonet.app.data.model.HomeFeed
import com.melonet.app.data.remote.HomeApi
import kotlinx.coroutines.withContext

class HomeRepository(
    private val homeApi: HomeApi,
    private val dispatchers: DispatchersProvider
) {
    suspend fun getHomeFeed(): Result<HomeFeed> = withContext(dispatchers.io) {
        when (val result = safeApiCall { homeApi.getHomeFeed() }) {
            is Result.Success -> Result.Success(HomeMapper.toModel(result.data))
            is Result.Error -> result
        }
    }
}
