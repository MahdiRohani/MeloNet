package com.melonet.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.mapper.SongMapper
import com.melonet.app.data.model.Song
import com.melonet.app.data.remote.CatalogApi
import com.melonet.app.data.remote.dto.SongDto
import java.io.IOException

enum class CatalogListType {
    POPULAR,
    NEW,
    TRENDING,
    CATEGORY,
}

class CatalogPagingSource(
    private val catalogApi: CatalogApi,
    private val listType: CatalogListType,
    private val category: String? = null,
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
            val response: ApiResponse<List<SongDto>> = when (listType) {
                CatalogListType.POPULAR -> catalogApi.getPopular(page = page, limit = params.loadSize)
                CatalogListType.NEW -> catalogApi.getNewReleases(page = page, limit = params.loadSize)
                CatalogListType.TRENDING -> catalogApi.getTrending(page = page, limit = params.loadSize)
                CatalogListType.CATEGORY -> catalogApi.getSongs(
                    category = category,
                    sort = null,
                    page = page,
                    limit = params.loadSize,
                )
            }
            response.error?.let { return LoadResult.Error(IOException(it.message)) }
            val items = response.data?.map(SongMapper::toModel).orEmpty()
            val hasMore = response.meta?.hasMore == true && items.isNotEmpty()
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
