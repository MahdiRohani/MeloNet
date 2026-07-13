package com.melonet.app.feature.search

import com.melonet.app.data.model.SearchFilter
import com.melonet.app.data.repository.SearchRepository
import com.melonet.app.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun queryChanged_updatesState() = runTest {
        val repository = mockk<SearchRepository>(relaxed = true)
        val viewModel = SearchViewModel(repository)

        viewModel.handleEvent(SearchContract.Event.QueryChanged("melo"))

        assertEquals("melo", viewModel.uiState.value.query)
    }

    @Test
    fun queryChanged_doesNotSaveHistory() = runTest {
        val repository = mockk<SearchRepository>(relaxed = true)
        val viewModel = SearchViewModel(repository)

        viewModel.handleEvent(SearchContract.Event.QueryChanged("m"))
        viewModel.handleEvent(SearchContract.Event.QueryChanged("mo"))
        viewModel.handleEvent(SearchContract.Event.QueryChanged("moh"))

        coVerify(exactly = 0) { repository.saveHistory(any()) }
    }

    @Test
    fun querySubmitted_savesHistory() = runTest {
        val repository = mockk<SearchRepository>(relaxed = true)
        val viewModel = SearchViewModel(repository)

        viewModel.handleEvent(SearchContract.Event.QuerySubmitted("mohsen"))

        assertEquals("mohsen", viewModel.uiState.value.query)
        coVerify(exactly = 1) { repository.saveHistory("mohsen") }
    }

    @Test
    fun filterSelected_updatesState() = runTest {
        val repository = mockk<SearchRepository>(relaxed = true)
        val viewModel = SearchViewModel(repository)

        viewModel.handleEvent(SearchContract.Event.FilterSelected(SearchFilter.SONG))

        assertEquals(SearchFilter.SONG, viewModel.uiState.value.selectedFilter)
    }
}
