package com.melonet.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.melonet.app.data.model.Song
import com.melonet.app.data.paging.CatalogListType
import com.melonet.app.data.paging.CatalogPagingSource
import com.melonet.app.data.remote.CatalogApi
import kotlinx.coroutines.flow.Flow

class CatalogRepository(
    private val catalogApi: CatalogApi,
) {
    fun catalogSongs(listType: CatalogListType, category: String? = null): Flow<PagingData<Song>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            CatalogPagingSource(
                catalogApi = catalogApi,
                listType = listType,
                category = category,
            )
        },
    ).flow
}
