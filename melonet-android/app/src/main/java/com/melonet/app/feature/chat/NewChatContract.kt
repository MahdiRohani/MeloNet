package com.melonet.app.feature.chat

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.SearchUser

object NewChatContract {

    data class State(
        val query: String = "",
        val results: List<SearchUser> = emptyList(),
        val isSearching: Boolean = false,
        val hasSearched: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data class UserClicked(val user: SearchUser) : Event
    }

    sealed interface Effect : UiEffect {
        data class OpenChat(val userId: Int, val displayName: String) : Effect
    }
}
