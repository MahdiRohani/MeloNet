package com.melonet.app.feature.voicecover

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.PlayerRepository
import com.melonet.app.data.repository.VoiceCoverRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoiceCoverCreateViewModel(
    private val playerRepository: PlayerRepository,
    private val voiceCoverRepository: VoiceCoverRepository,
) : BaseViewModel<VoiceCoverCreateContract.State, VoiceCoverCreateContract.Event, VoiceCoverCreateContract.Effect>() {

    private var songId: String = ""
    private var pollJob: Job? = null

    override fun createInitialState() = VoiceCoverCreateContract.State()

    override fun handleEvent(event: VoiceCoverCreateContract.Event) {
        when (event) {
            is VoiceCoverCreateContract.Event.Load -> {
                songId = event.songId
                load()
            }
            VoiceCoverCreateContract.Event.Retry -> load()
            is VoiceCoverCreateContract.Event.ArtistSelected -> submit(event.slug)
        }
    }

    private fun load() {
        if (songId.isBlank()) return
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val songResult = playerRepository.getSong(songId)
            val artistsResult = voiceCoverRepository.listArtists()
            when {
                songResult is Result.Error -> {
                    setState {
                        copy(isLoading = false, error = mapError(songResult.error))
                    }
                }
                artistsResult is Result.Error -> {
                    setState {
                        copy(isLoading = false, error = mapError(artistsResult.error))
                    }
                }
                songResult is Result.Success && artistsResult is Result.Success -> {
                    setState {
                        copy(
                            song = songResult.data,
                            artists = artistsResult.data.filter { it.enabled },
                            isLoading = false,
                            error = null,
                        )
                    }
                }
            }
        }
    }

    private fun submit(slug: String) {
        if (songId.isBlank()) return
        if (uiState.value.isSubmitting || uiState.value.isProcessing) return
        viewModelScope.launch {
            setState {
                copy(
                    isSubmitting = true,
                    selectedSlug = slug,
                    error = null,
                    activeCover = null,
                )
            }
            when (val result = voiceCoverRepository.create(songId, slug)) {
                is Result.Success -> {
                    val cover = result.data
                    setState {
                        copy(
                            isSubmitting = false,
                            activeCover = cover,
                        )
                    }
                    when {
                        cover.isReady -> {
                            setEffect { VoiceCoverCreateContract.Effect.OpenPlayer(cover.id) }
                        }
                        cover.isFailed -> {
                            setState {
                                copy(error = friendlyCoverError(cover.error))
                            }
                        }
                        else -> startPolling(cover.id)
                    }
                }
                is Result.Error -> {
                    setState {
                        copy(
                            isSubmitting = false,
                            error = mapError(result.error),
                        )
                    }
                }
            }
        }
    }

    private fun startPolling(coverId: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                when (val result = voiceCoverRepository.get(coverId)) {
                    is Result.Success -> {
                        val cover = result.data
                        setState { copy(activeCover = cover) }
                        when {
                            cover.isReady -> {
                                setEffect { VoiceCoverCreateContract.Effect.OpenPlayer(cover.id) }
                                return@launch
                            }
                            cover.isFailed -> {
                                setState {
                                    copy(
                                        error = friendlyCoverError(cover.error),
                                        isSubmitting = false,
                                    )
                                }
                                return@launch
                            }
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

    private fun friendlyCoverError(raw: String): String {
        val msg = raw.lowercase()
        return when {
            "model missing" in msg || "rvc model" in msg -> "model_unavailable"
            msg.isBlank() -> "failed"
            else -> "failed"
        }
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
