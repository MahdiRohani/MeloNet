package com.melonet.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.melonet.app.data.mapper.ChatMapper
import com.melonet.app.data.model.Conversation
import com.melonet.app.data.remote.ChatApi
import java.io.IOException

class ConversationsPagingSource(
    private val chatApi: ChatApi,
) : PagingSource<Int, Conversation>() {

    override fun getRefreshKey(state: PagingState<Int, Conversation>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Conversation> {
        val page = params.key ?: 1
        return try {
            val response = chatApi.listConversations(page = page, limit = params.loadSize)
            val error = response.error
            if (error != null) {
                return LoadResult.Error(IOException(error.message))
            }
            val data = response.data ?: return LoadResult.Error(IOException("Empty response"))
            val items = data.map(ChatMapper::toConversation)
            val total = response.meta?.total ?: items.size
            val hasMore = page * params.loadSize < total
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
