package com.melonet.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.melonet.app.data.mapper.SearchMapper
import com.melonet.app.data.model.SearchFilter
import com.melonet.app.data.model.SearchResultItem
import com.melonet.app.data.remote.SearchApi
import java.io.IOException

class SearchPagingSource(
    private val searchApi: SearchApi,
    private val query: String,
    private val filter: SearchFilter,
) : PagingSource<Int, SearchResultItem>() {

    override fun getRefreshKey(state: PagingState<Int, SearchResultItem>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResultItem> {
        val page = params.key ?: 1
        return try {
            val response = searchApi.search(
                query = query,
                type = filter.apiValue,
                page = page,
                limit = params.loadSize,
            )
            val error = response.error
            if (error != null) {
                return LoadResult.Error(IOException(error.message))
            }
            val data = response.data
                ?: return LoadResult.Error(IOException("Empty search response"))
            val items = SearchMapper.toResultItems(data, filter)
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
