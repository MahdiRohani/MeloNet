package com.melonet.app.feature.downloads

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.DownloadItem
import com.melonet.app.data.model.DownloadSort

object DownloadsContract {

    data class State(
        val isPremium: Boolean = false,
        val sort: DownloadSort = DownloadSort.NEWEST,
        val downloads: List<DownloadItem> = emptyList(),
    ) : UiState

    sealed interface Event : UiEvent {
        data class SortChanged(val sort: DownloadSort) : Event
        data class PlayDownload(val item: DownloadItem) : Event
        data class DeleteDownload(val songId: String) : Event
        data class RetryDownload(val songId: String) : Event
        data object UpgradePremiumClicked : Event
    }

    sealed interface Effect : UiEffect {
        data class PlaySong(val item: DownloadItem) : Effect
        data object NavigateToProfile : Effect
    }
}
