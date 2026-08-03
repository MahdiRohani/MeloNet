package com.melonet.app.feature.voicecover

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.VoiceCover
import com.melonet.app.data.repository.SearchRepository
import com.melonet.app.data.repository.VoiceCoverRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VoiceCoverViewModel(
    private val voiceCoverRepository: VoiceCoverRepository,
    private val searchRepository: SearchRepository,
) : BaseViewModel<VoiceCoverContract.State, VoiceCoverContract.Event, VoiceCoverContract.Effect>() {

    private var searchJob: Job? = null

    override fun createInitialState() = VoiceCoverContract.State()

    init {
        loadCatalog()
    }

    override fun handleEvent(event: VoiceCoverContract.Event) {
        when (event) {
            is VoiceCoverContract.Event.QueryChanged -> {
                setState { copy(query = event.query, searchError = null) }
                scheduleSearch(event.query)
            }
            VoiceCoverContract.Event.Submit -> runSearch(uiState.value.query)
            VoiceCoverContract.Event.Refresh -> loadCatalog()
            is VoiceCoverContract.Event.CoverClicked -> {
                setEffect { VoiceCoverContract.Effect.OpenPlayer(event.cover.id) }
            }
            is VoiceCoverContract.Event.CoverDelete -> deleteCover(event.cover)
            is VoiceCoverContract.Event.SongClicked -> {
                setEffect { VoiceCoverContract.Effect.OpenCreate(event.song.id) }
            }
        }
    }

    private fun deleteCover(cover: VoiceCover) {
        viewModelScope.launch {
            val previous = uiState.value.readyCovers
            setState { copy(readyCovers = readyCovers.filterNot { it.id == cover.id }) }
            when (val result = voiceCoverRepository.delete(cover.id)) {
                is Result.Success -> Unit
                is Result.Error -> {
                    // Keep removed if server already deleted it.
                    if (result.error is AppError.NotFound) return@launch
                    setState { copy(readyCovers = previous) }
                    loadCatalog()
                }
            }
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            setState { copy(isLoadingCatalog = readyCovers.isEmpty(), catalogError = null) }
            when (val result = voiceCoverRepository.listReady()) {
                is Result.Success -> {
                    setState {
                        copy(
                            readyCovers = result.data,
                            isLoadingCatalog = false,
                            catalogError = null,
                        )
                    }
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoadingCatalog = false,
                            catalogError = mapError(result.error),
                        )
                    }
                }
            }
        }
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            setState {
                copy(
                    results = emptyList(),
                    isSearching = false,
                    hasSearched = false,
                    searchError = null,
                )
            }
            return
        }
        setState { copy(isSearching = true, hasSearched = false, searchError = null) }
        searchJob = viewModelScope.launch {
            delay(450)
            runSearch(trimmed)
        }
    }

    private fun runSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            setState { copy(isSearching = true, searchError = null) }
            when (val result = searchRepository.searchSongsResult(trimmed, limit = 30)) {
                is Result.Success -> {
                    setState {
                        copy(
                            results = result.data,
                            isSearching = false,
                            hasSearched = true,
                            searchError = null,
                        )
                    }
                }
                is Result.Error -> {
                    setState {
                        copy(
                            results = emptyList(),
                            isSearching = false,
                            hasSearched = true,
                            searchError = mapError(result.error),
                        )
                    }
                }
            }
        }
    }

    private fun mapError(error: AppError): String = when (error) {
        is AppError.Http -> when (error.code) {
            "rate_limited" -> "rate_limited"
            else -> error.code
        }
        AppError.NoConnection -> "no_connection"
        AppError.Timeout -> "timeout"
        else -> "failed"
    }
}
