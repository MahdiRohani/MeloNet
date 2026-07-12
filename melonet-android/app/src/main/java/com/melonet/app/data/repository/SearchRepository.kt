package com.melonet.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.data.local.SearchHistoryDao
import com.melonet.app.data.local.SearchHistoryEntity
import com.melonet.app.data.model.SearchFilter
import com.melonet.app.data.model.SearchResultItem
import com.melonet.app.data.paging.SearchPagingSource
import com.melonet.app.data.remote.SearchApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SearchRepository(
    private val searchApi: SearchApi,
    private val searchHistoryDao: SearchHistoryDao,
    private val dispatchers: DispatchersProvider,
) {
    fun observeHistory(): Flow<List<String>> =
        searchHistoryDao.observeRecent().mapToQueries()

    fun searchResults(query: String, filter: SearchFilter): Flow<PagingData<SearchResultItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE,
            ),
            pagingSourceFactory = {
                SearchPagingSource(
                    searchApi = searchApi,
                    query = query,
                    filter = filter,
                )
            },
        ).flow
    }

    suspend fun saveHistory(query: String) = withContext(dispatchers.io) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext
        searchHistoryDao.upsert(SearchHistoryEntity(query = trimmed))
    }

    suspend fun deleteHistory(query: String) = withContext(dispatchers.io) {
        searchHistoryDao.delete(query)
    }

    private fun Flow<List<SearchHistoryEntity>>.mapToQueries(): Flow<List<String>> =
        map { entities -> entities.map { it.query } }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
