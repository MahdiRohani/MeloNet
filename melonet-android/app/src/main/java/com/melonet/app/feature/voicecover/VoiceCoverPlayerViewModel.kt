package com.melonet.app.feature.voicecover

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.VoiceCoverRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoiceCoverPlayerViewModel(
    private val voiceCoverRepository: VoiceCoverRepository,
) : BaseViewModel<VoiceCoverPlayerContract.State, VoiceCoverPlayerContract.Event, VoiceCoverPlayerContract.Effect>() {

    private var coverId: Long = 0L
    private var pollJob: Job? = null

    override fun createInitialState() = VoiceCoverPlayerContract.State()

    override fun handleEvent(event: VoiceCoverPlayerContract.Event) {
        when (event) {
            is VoiceCoverPlayerContract.Event.Load -> {
                coverId = event.coverId
                load()
            }
            VoiceCoverPlayerContract.Event.Retry -> load()
            VoiceCoverPlayerContract.Event.Play -> {
                val cover = uiState.value.cover
                if (cover?.isReady == true) {
                    setEffect { VoiceCoverPlayerContract.Effect.PlayCover(cover) }
                    setState { copy(autoPlayed = true) }
                }
            }
            VoiceCoverPlayerContract.Event.Delete -> deleteCover()
        }
    }

    private fun deleteCover() {
        if (coverId <= 0L) return
        viewModelScope.launch {
            pollJob?.cancel()
            when (val result = voiceCoverRepository.delete(coverId)) {
                is Result.Success -> setEffect { VoiceCoverPlayerContract.Effect.Deleted }
                is Result.Error -> setState { copy(error = mapError(result.error)) }
            }
        }
    }

    private fun load() {
        if (coverId <= 0L) return
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = voiceCoverRepository.get(coverId)) {
                is Result.Success -> {
                    val cover = result.data
                    setState {
                        copy(
                            cover = cover,
                            isLoading = false,
                            isPolling = cover.isInProgress,
                            error = if (cover.isFailed) cover.error.ifBlank { "failed" } else null,
                        )
                    }
                    when {
                        cover.isReady && !uiState.value.autoPlayed -> {
                            setEffect { VoiceCoverPlayerContract.Effect.PlayCover(cover) }
                            setState { copy(autoPlayed = true) }
                        }
                        cover.isInProgress -> startPolling()
                    }
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isLoading = false,
                            error = mapError(result.error),
                        )
                    }
                }
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                when (val result = voiceCoverRepository.get(coverId)) {
                    is Result.Success -> {
                        val cover = result.data
                        setState {
                            copy(
                                cover = cover,
                                isPolling = cover.isInProgress,
                                error = if (cover.isFailed) cover.error.ifBlank { "failed" } else null,
                            )
                        }
                        when {
                            cover.isReady -> {
                                if (!uiState.value.autoPlayed) {
                                    setEffect { VoiceCoverPlayerContract.Effect.PlayCover(cover) }
                                    setState { copy(autoPlayed = true, isPolling = false) }
                                }
                                return@launch
                            }
                            cover.isFailed -> return@launch
                        }
                    }
                    is Result.Error -> {
                        setState { copy(error = mapError(result.error)) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    private fun mapError(error: AppError): String = when (error) {
        AppError.NoConnection -> "no_connection"
        AppError.Timeout -> "timeout"
        AppError.NotFound -> "not_found"
        else -> "failed"
    }

    companion object {
        private const val POLL_INTERVAL_MS = 3_000L
    }
}
