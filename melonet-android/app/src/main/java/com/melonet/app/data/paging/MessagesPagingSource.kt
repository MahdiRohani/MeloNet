package com.melonet.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.melonet.app.data.local.ChatMessageDao
import com.melonet.app.data.mapper.ChatMapper
import com.melonet.app.data.model.ChatMessage
import com.melonet.app.data.remote.ChatApi
import java.io.IOException

class MessagesPagingSource(
    private val chatApi: ChatApi,
    private val chatMessageDao: ChatMessageDao,
    private val conversationId: Int,
    private val currentUserId: Int,
) : PagingSource<Int, ChatMessage>() {

    override fun getRefreshKey(state: PagingState<Int, ChatMessage>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChatMessage> {
        val pageSize = params.loadSize.coerceAtLeast(MIN_PAGE_SIZE)
        return try {
            if (params.key == null) {
                val probe = chatApi.getMessages(conversationId, page = 1, limit = pageSize)
                probe.error?.let { return LoadResult.Error(IOException(it.message)) }
                val total = probe.meta?.total ?: 0
                if (total == 0) {
                    return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
                }
                val lastPage = ((total + pageSize - 1) / pageSize).coerceAtLeast(1)
                return loadPage(lastPage, pageSize, total)
            }
            loadPage(params.key!!, pageSize, null)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun loadPage(
        page: Int,
        pageSize: Int,
        knownTotal: Int?,
    ): LoadResult<Int, ChatMessage> {
        val response = chatApi.getMessages(conversationId, page = page, limit = pageSize)
        response.error?.let { return LoadResult.Error(IOException(it.message)) }
        val dtos = response.data ?: return LoadResult.Error(IOException("Empty response"))
        val messages = dtos.map { ChatMapper.toMessage(it, currentUserId) }
        chatMessageDao.upsertAll(messages.map(ChatMapper::toEntity))
        val total = knownTotal ?: response.meta?.total ?: messages.size
        val totalPages = ((total + pageSize - 1) / pageSize).coerceAtLeast(1)
        return LoadResult.Page(
            data = messages,
            prevKey = if (page > 1) page - 1 else null,
            nextKey = if (page < totalPages) page + 1 else null,
        )
    }

    private companion object {
        const val MIN_PAGE_SIZE = 30
    }
}
