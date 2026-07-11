package com.melonet.app.domain.repository

import com.melonet.app.core.common.Result
import com.melonet.app.domain.model.HomeFeed

interface HomeRepository {
    suspend fun getHomeFeed(): Result<HomeFeed>
}
