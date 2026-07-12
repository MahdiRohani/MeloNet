package com.melonet.app.feature.downloads

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.DownloadSort
import com.melonet.app.data.repository.DownloadRepository
import com.melonet.app.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
    private val userRepository: UserRepository,
) : BaseViewModel<DownloadsContract.State, DownloadsContract.Event, DownloadsContract.Effect>() {

    private val sortFlow = MutableStateFlow(DownloadSort.NEWEST)

    override fun createInitialState() = DownloadsContract.State()

    init {
        userRepository.isPremiumFlow
            .onEach { isPremium -> setState { copy(isPremium = isPremium) } }
            .launchIn(viewModelScope)

        sortFlow
            .flatMapLatest { sort -> downloadRepository.observeDownloads(sort) }
            .onEach { downloads -> setState { copy(downloads = downloads) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: DownloadsContract.Event) {
        when (event) {
            is DownloadsContract.Event.SortChanged -> {
                sortFlow.value = event.sort
                setState { copy(sort = event.sort) }
            }
            is DownloadsContract.Event.PlayDownload -> {
                setEffect { DownloadsContract.Effect.PlaySong(event.item) }
            }
            is DownloadsContract.Event.DeleteDownload -> deleteDownload(event.songId)
            is DownloadsContract.Event.RetryDownload -> retryDownload(event.songId)
            DownloadsContract.Event.UpgradePremiumClicked -> upgradePremium()
        }
    }

    private fun deleteDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(songId)
        }
    }

    private fun retryDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.retryDownload(songId)
        }
    }

    private fun upgradePremium() {
        viewModelScope.launch {
            userRepository.setPremiumStatus(true)
            setEffect { DownloadsContract.Effect.NavigateToProfile }
        }
    }
}
