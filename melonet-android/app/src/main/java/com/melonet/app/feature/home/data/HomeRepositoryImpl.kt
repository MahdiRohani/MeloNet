package com.melonet.app.feature.home.data

import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.common.Result
import com.melonet.app.core.network.safeApiCall
import com.melonet.app.domain.model.HomeFeed
import com.melonet.app.domain.repository.HomeRepository
import com.melonet.app.feature.home.data.mapper.HomeMapper
import com.melonet.app.feature.home.data.remote.HomeApi
import kotlinx.coroutines.withContext

class HomeRepositoryImpl(
    private val homeApi: HomeApi,
    private val dispatchers: DispatchersProvider
) : HomeRepository {

    override suspend fun getHomeFeed(): Result<HomeFeed> = withContext(dispatchers.io) {
        safeApiCall { homeApi.getHomeFeed() }.let { result ->
            when (result) {
                is Result.Success -> Result.Success(HomeMapper.toDomain(result.data))
                is Result.Error -> result
            }
        }
    }
}
