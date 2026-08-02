package com.melonet.app.feature.downloads

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.local.SettingsRepository
import com.melonet.app.data.model.DownloadSort
import com.melonet.app.data.model.DownloadStatus
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
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<DownloadsContract.State, DownloadsContract.Event, DownloadsContract.Effect>() {

    private val sortFlow = MutableStateFlow(DownloadSort.NEWEST)

    override fun createInitialState() = DownloadsContract.State()

    init {
        userRepository.isPremiumFlow
            .onEach { isPremium -> setState { copy(isPremium = isPremium) } }
            .launchIn(viewModelScope)

        settingsRepository.downloadsWifiOnlyFlow
            .onEach { wifiOnly -> setState { copy(downloadsWifiOnly = wifiOnly) } }
            .launchIn(viewModelScope)

        sortFlow
            .flatMapLatest { sort -> downloadRepository.observeDownloads(sort) }
            .onEach { downloads ->
                setState { copy(downloads = downloads) }
                refreshStorage()
            }
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
            is DownloadsContract.Event.WifiOnlyChanged -> {
                viewModelScope.launch {
                    settingsRepository.setDownloadsWifiOnly(event.enabled)
                }
            }
        }
    }

    private fun refreshStorage() {
        viewModelScope.launch {
            val bytes = downloadRepository.storageUsedBytes()
            setState { copy(storageUsedBytes = bytes) }
        }
    }

    private fun deleteDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(songId)
            refreshStorage()
        }
    }

    private fun retryDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.retryDownload(songId)
        }
    }

    private fun upgradePremium() {
        viewModelScope.launch {
            when (val result = userRepository.activatePremium()) {
                is Result.Success -> {
                    // Stay on Downloads so the unlocked list is immediately usable.
                }
                is Result.Error -> Unit
            }
        }
    }
}

fun formatStorageBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}

fun DownloadsContract.State.completedCount(): Int =
    downloads.count { it.status == DownloadStatus.COMPLETED }
