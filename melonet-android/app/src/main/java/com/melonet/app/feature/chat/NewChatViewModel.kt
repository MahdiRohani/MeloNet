package com.melonet.app.feature.chat

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.model.SearchUser
import com.melonet.app.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NewChatViewModel(
    private val searchRepository: SearchRepository,
) : BaseViewModel<NewChatContract.State, NewChatContract.Event, NewChatContract.Effect>() {

    private var searchJob: Job? = null

    override fun createInitialState() = NewChatContract.State()

    override fun handleEvent(event: NewChatContract.Event) {
        when (event) {
            is NewChatContract.Event.QueryChanged -> {
                setState { copy(query = event.query) }
                scheduleSearch(event.query)
            }
            is NewChatContract.Event.UserClicked -> {
                setEffect {
                    NewChatContract.Effect.OpenChat(
                        userId = event.user.id,
                        displayName = event.user.displayName,
                    )
                }
            }
        }
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            setState { copy(results = emptyList(), isSearching = false, hasSearched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            setState { copy(isSearching = true) }
            val users = searchRepository.searchUsers(query.trim(), limit = 30)
            setState { copy(results = users, isSearching = false, hasSearched = true) }
        }
    }
}
