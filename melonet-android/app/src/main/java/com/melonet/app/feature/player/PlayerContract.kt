package com.melonet.app.feature.player

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.DownloadStatus
import com.melonet.app.data.model.Lyrics
import com.melonet.app.data.model.Playlist
import com.melonet.app.data.model.RepeatMode
import com.melonet.app.data.model.Song

object PlayerContract {

    data class State(
        val currentSong: Song? = null,
        val queue: List<Song> = emptyList(),
        val currentIndex: Int = -1,
        val isPlaying: Boolean = false,
        val isLoading: Boolean = false,
        val isSeeking: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val playbackSpeed: Float = 1f,
        val sleepTimerMinutesLeft: Int? = null,
        val showSleepTimerDialog: Boolean = false,
        val gradientColors: List<Long> = emptyList(),
        val isPremium: Boolean = false,
        val downloadStatus: DownloadStatus? = null,
        val showUpgradeDialog: Boolean = false,
        val shuffleEnabled: Boolean = false,
        val repeatMode: RepeatMode = RepeatMode.ALL,
        val isLiked: Boolean = false,
        val showMoreMenu: Boolean = false,
        val showShareSheet: Boolean = false,
        val showAddToPlaylistDialog: Boolean = false,
        val showQueueSheet: Boolean = false,
        val showLyricsSheet: Boolean = false,
        val playlists: List<Playlist> = emptyList(),
        val lyrics: Lyrics = Lyrics.EMPTY,
        val lyricsReady: Boolean = false,
        val isLoadingLyrics: Boolean = false,
        val lyricsOffsetMs: Long = 0L,
        val currentLyricLineIndex: Int = -1,
    ) : UiState

    sealed interface Event : UiEvent {
        data class PlaySong(val song: Song, val queue: List<Song> = listOf()) : Event
        data class PlaySongId(val songId: String, val queue: List<Song> = emptyList()) : Event
        data object TogglePlayPause : Event
        data object SkipNext : Event
        data object SkipPrevious : Event
        data class SeekTo(val positionMs: Long) : Event
        data object CycleSpeed : Event
        data class SetSleepTimer(val minutes: Int?) : Event
        data object ShowSleepTimerDialog : Event
        data object HideSleepTimerDialog : Event
        data class UpdateGradient(val colors: List<Long>) : Event
        data object DownloadClicked : Event
        data object DismissUpgradeDialog : Event
        data object ToggleShuffle : Event
        data object CycleRepeatMode : Event
        data object ToggleLike : Event
        data object ShowMoreMenu : Event
        data object HideMoreMenu : Event
        data object ShowShareSheet : Event
        data object HideShareSheet : Event
        data object ShowAddToPlaylistDialog : Event
        data object HideAddToPlaylistDialog : Event
        data class AddToPlaylist(val playlistId: Int) : Event
        data object GoToArtist : Event
        data object ShareExternal : Event
        data object ShareToChat : Event
        data object ShowQueueSheet : Event
        data object HideQueueSheet : Event
        data object ShowLyricsSheet : Event
        data object HideLyricsSheet : Event
        data class PlayQueueIndex(val index: Int) : Event
        data class RemoveFromQueue(val songId: String) : Event
        data class MoveInQueue(val fromIndex: Int, val toIndex: Int) : Event
        data class SeekToLyricLine(val index: Int) : Event
        data class AdjustLyricsOffset(val deltaMs: Long) : Event
    }

    sealed interface Effect : UiEffect {
        data object NavigateBack : Effect
        data class ShareExternal(
            val uri: android.net.Uri,
            val mimeType: String,
            val text: String,
        ) : Effect
        data class ShareToChat(val songId: String) : Effect
        data class NavigateToArtist(val artistId: Int) : Effect
        data class ShowMessage(val message: String) : Effect
        data class ShowError(val message: String) : Effect
    }
}
