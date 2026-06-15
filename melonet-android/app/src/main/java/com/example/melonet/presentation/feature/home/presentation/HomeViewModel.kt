package com.example.melonet.presentation.feature.home.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melonet.data.model.SongDto
import com.example.melonet.presentation.feature.home.data.HomeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = true,
    val newSongs: List<SongDto> = emptyList(),
    val popularSongs: List<SongDto> = emptyList(),
    val iranianSongs: List<SongDto> = emptyList(),
    val globalSongs: List<SongDto> = emptyList(),
    val error: String? = null
)

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        fetchHomeData()
    }

    private fun fetchHomeData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val newSongsDeferred = async { repository.getSongsByCategory("New") }
            val popularSongsDeferred = async { repository.getSongsByCategory("Popular") }
            val iranianSongsDeferred = async { repository.getSongsByCategory("Iranian") }
            val globalSongsDeferred = async { repository.getSongsByCategory("Global") }

            _state.update {
                it.copy(
                    isLoading = false,
                    newSongs = newSongsDeferred.await(),
                    popularSongs = popularSongsDeferred.await(),
                    iranianSongs = iranianSongsDeferred.await(),
                    globalSongs = globalSongsDeferred.await()
                )
            }
        }
    }
}