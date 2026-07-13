package com.melonet.app.feature.player

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.DownloadStatus
import com.melonet.app.data.repository.DownloadRepository
import com.melonet.app.data.repository.LibraryRepository
import com.melonet.app.data.repository.PlaylistRepository
import com.melonet.app.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val playbackManager: PlaybackManager,
    private val downloadRepository: DownloadRepository,
    private val userRepository: UserRepository,
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
) : BaseViewModel<PlayerContract.State, PlayerContract.Event, PlayerContract.Effect>() {

    private val speedSteps = listOf(1f, 1.25f, 1.5f, 2f, 0.5f, 0.75f)

    override fun createInitialState() = PlayerContract.State()

    init {
        playbackManager.connect()
        playbackManager.state
            .onEach { playback ->
                setState {
                    copy(
                        currentSong = playback.currentSong,
                        queue = playback.queue,
                        isPlaying = playback.isPlaying,
                        isLoading = playback.isLoading,
                        positionMs = playback.positionMs,
                        durationMs = playback.durationMs,
                        playbackSpeed = playback.playbackSpeed,
                        sleepTimerMinutesLeft = playback.sleepTimerMinutesLeft,
                        shuffleEnabled = playback.shuffleEnabled,
                        repeatMode = playback.repeatMode,
                    )
                }
            }
            .launchIn(viewModelScope)

        userRepository.isPremiumFlow
            .onEach { isPremium -> setState { copy(isPremium = isPremium) } }
            .launchIn(viewModelScope)

        val songIdFlow = uiState
            .map { it.currentSong?.id }
            .distinctUntilChanged()

        songIdFlow
            .flatMapLatest { songId ->
                if (songId == null) {
                    flowOf(null)
                } else {
                    downloadRepository.observeDownload(songId).map { it?.status }
                }
            }
            .onEach { status -> setState { copy(downloadStatus = status) } }
            .launchIn(viewModelScope)

        songIdFlow
            .flatMapLatest { songId ->
                if (songId == null) flowOf(false) else libraryRepository.observeIsLiked(songId)
            }
            .onEach { liked -> setState { copy(isLiked = liked) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: PlayerContract.Event) {
        when (event) {
            is PlayerContract.Event.PlaySong -> {
                val queue = event.queue.ifEmpty { listOf(event.song) }
                playbackManager.play(event.song, queue)
            }
            is PlayerContract.Event.PlaySongId -> {
                playbackManager.playSongId(event.songId, event.queue)
            }
            PlayerContract.Event.TogglePlayPause -> playbackManager.togglePlayPause()
            PlayerContract.Event.SkipNext -> playbackManager.skipNext()
            PlayerContract.Event.SkipPrevious -> playbackManager.skipPrevious()
            is PlayerContract.Event.SeekTo -> playbackManager.seekTo(event.positionMs)
            PlayerContract.Event.CycleSpeed -> cycleSpeed()
            is PlayerContract.Event.SetSleepTimer -> {
                playbackManager.setSleepTimer(event.minutes)
                setState { copy(showSleepTimerDialog = false) }
            }
            PlayerContract.Event.ShowSleepTimerDialog -> setState { copy(showSleepTimerDialog = true) }
            PlayerContract.Event.HideSleepTimerDialog -> setState { copy(showSleepTimerDialog = false) }
            is PlayerContract.Event.UpdateGradient -> setState { copy(gradientColors = event.colors) }
            PlayerContract.Event.DownloadClicked -> handleDownloadClick()
            PlayerContract.Event.DismissUpgradeDialog -> setState { copy(showUpgradeDialog = false) }
            PlayerContract.Event.ToggleShuffle -> playbackManager.toggleShuffle()
            PlayerContract.Event.CycleRepeatMode -> playbackManager.cycleRepeatMode()
            PlayerContract.Event.ToggleLike -> toggleLike()
            PlayerContract.Event.ShowMoreMenu -> setState { copy(showMoreMenu = true) }
            PlayerContract.Event.HideMoreMenu -> setState { copy(showMoreMenu = false) }
            PlayerContract.Event.ShowShareSheet -> setState { copy(showShareSheet = true, showMoreMenu = false) }
            PlayerContract.Event.HideShareSheet -> setState { copy(showShareSheet = false) }
            PlayerContract.Event.ShowAddToPlaylistDialog -> openAddToPlaylist()
            PlayerContract.Event.HideAddToPlaylistDialog -> setState { copy(showAddToPlaylistDialog = false) }
            is PlayerContract.Event.AddToPlaylist -> addToPlaylist(event.playlistId)
            PlayerContract.Event.GoToArtist -> goToArtist()
            PlayerContract.Event.ShareExternal -> shareExternal()
            PlayerContract.Event.ShareToChat -> shareToChat()
        }
    }

    fun playIfNeeded(songId: String) {
        viewModelScope.launch {
            val current = uiState.value.currentSong
            if (current?.id != songId) {
                handleEvent(PlayerContract.Event.PlaySongId(songId))
            }
        }
    }

    private fun cycleSpeed() {
        val current = uiState.value.playbackSpeed
        val currentIndex = speedSteps.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
        val next = speedSteps[(currentIndex + 1) % speedSteps.size]
        playbackManager.setSpeed(next)
    }

    private fun toggleLike() {
        val song = uiState.value.currentSong ?: return
        val liked = uiState.value.isLiked
        viewModelScope.launch {
            if (liked) libraryRepository.unlikeSong(song.id) else libraryRepository.likeSong(song)
        }
    }

    private fun openAddToPlaylist() {
        setState { copy(showMoreMenu = false, showAddToPlaylistDialog = true) }
        viewModelScope.launch {
            when (val result = playlistRepository.getUserPlaylists()) {
                is Result.Success -> setState { copy(playlists = result.data) }
                is Result.Error -> Unit
            }
        }
    }

    private fun addToPlaylist(playlistId: Int) {
        val song = uiState.value.currentSong ?: return
        viewModelScope.launch {
            playlistRepository.addLocalSongToPlaylist(playlistId, song)
            setState { copy(showAddToPlaylistDialog = false) }
            setEffect { PlayerContract.Effect.ShowMessage(song.title) }
        }
    }

    private fun goToArtist() {
        val song = uiState.value.currentSong ?: return
        setState { copy(showMoreMenu = false) }
        setEffect { PlayerContract.Effect.NavigateToArtist(syntheticArtistId(song.artistName)) }
    }

    private fun shareExternal() {
        val song = uiState.value.currentSong ?: return
        setState { copy(showShareSheet = false) }
        setEffect { PlayerContract.Effect.ShareExternal("${song.title} — ${song.artistName}") }
    }

    private fun shareToChat() {
        val song = uiState.value.currentSong ?: return
        setState { copy(showShareSheet = false) }
        setEffect { PlayerContract.Effect.ShareToChat(song.id) }
    }

    private fun handleDownloadClick() {
        val song = uiState.value.currentSong ?: return
        if (!uiState.value.isPremium) {
            setState { copy(showUpgradeDialog = true) }
            return
        }
        when (uiState.value.downloadStatus) {
            DownloadStatus.COMPLETED, DownloadStatus.DOWNLOADING, DownloadStatus.PENDING -> return
            DownloadStatus.FAILED, null -> viewModelScope.launch {
                downloadRepository.enqueueDownload(song)
            }
        }
    }

    fun upgradePremium() {
        viewModelScope.launch {
            userRepository.setPremiumStatus(true)
            setState { copy(isPremium = true, showUpgradeDialog = false) }
            uiState.value.currentSong?.let { song ->
                downloadRepository.enqueueDownload(song)
            }
        }
    }
}

/**
 * Mirrors the backend synthetic artist id: fnv-1a(lowercase(name)) % 2e9 + 1.
 */
fun syntheticArtistId(name: String): Int {
    val normalized = name.trim().lowercase()
    var hash = 2166136261L // FNV offset basis (32-bit)
    for (b in normalized.toByteArray(Charsets.UTF_8)) {
        hash = hash xor (b.toLong() and 0xFF)
        hash = (hash * 16777619L) and 0xFFFFFFFFL // FNV prime, keep 32-bit
    }
    return ((hash % 2_000_000_000L) + 1L).toInt()
}
