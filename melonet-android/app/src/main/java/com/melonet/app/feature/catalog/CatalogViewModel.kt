package com.melonet.app.feature.catalog

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.Song
import com.melonet.app.data.model.SortOption
import com.melonet.app.data.paging.CatalogListType
import com.melonet.app.data.repository.CatalogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val catalogRepository: CatalogRepository,
) : BaseViewModel<CatalogContract.State, CatalogContract.Event, CatalogContract.Effect>() {

    private data class CatalogArgs(
        val listType: String,
        val filter: String?,
        val sort: SortOption,
    )

    private val argsFlow = MutableStateFlow(
        CatalogArgs("popular", null, SortOption.MOST_PLAYED),
    )
    private val cachedSongs = mutableListOf<Song>()

    val songs: Flow<PagingData<Song>> = argsFlow
        .flatMapLatest { args ->
            catalogRepository.catalogSongs(
                listType = args.listType.toCatalogListType(),
                category = args.filter,
                sort = args.sort.apiValue,
            )
        }
        .cachedIn(viewModelScope)

    override fun createInitialState() = CatalogContract.State()

    fun configure(listType: String, filter: String?) {
        val defaultSort = if (listType.equals("new", ignoreCase = true)) {
            SortOption.NEWEST
        } else {
            SortOption.MOST_PLAYED
        }
        argsFlow.value = CatalogArgs(listType, filter, defaultSort)
        setState { copy(listType = listType, filter = filter, sort = defaultSort) }
    }

    override fun handleEvent(event: CatalogContract.Event) {
        when (event) {
            is CatalogContract.Event.SongClicked -> {
                setEffect { CatalogContract.Effect.PlayQueue(event.songId, shuffle = false) }
            }
            is CatalogContract.Event.SortSelected -> {
                if (event.sort == uiState.value.sort) return
                argsFlow.value = argsFlow.value.copy(sort = event.sort)
                setState { copy(sort = event.sort) }
            }
        }
    }

    fun updateCachedSongs(songs: List<Song>) {
        cachedSongs.clear()
        cachedSongs.addAll(songs)
    }

    fun getCachedSongs(): List<Song> = cachedSongs.toList()

    private fun String.toCatalogListType(): CatalogListType = when (lowercase()) {
        "popular" -> CatalogListType.POPULAR
        "new" -> CatalogListType.NEW
        "trending" -> CatalogListType.TRENDING
        else -> CatalogListType.CATEGORY
    }
}
