package com.melonet.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.melonet.app.core.network.ApiResponse
import com.melonet.app.data.mapper.SongMapper
import com.melonet.app.data.model.Song
import com.melonet.app.data.remote.ArtistApi
import com.melonet.app.data.remote.dto.SongDto

class ArtistSongsPagingSource(
    private val artistApi: ArtistApi,
    private val artistId: Int,
    private val sort: String? = null,
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
            val response: ApiResponse<List<SongDto>> =
                artistApi.getArtistSongs(id = artistId, sort = sort, page = page, limit = params.loadSize)
            response.error?.let { return LoadResult.Error(java.io.IOException(it.message)) }
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
