package com.melonet.app.feature.following

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Artist
import com.melonet.app.data.model.SearchUser
import com.melonet.app.data.model.SocialUser

object FollowingContract {

    data class State(
        val users: List<SocialUser> = emptyList(),
        val artists: List<Artist> = emptyList(),
        val query: String = "",
        val searchResults: List<SearchUser> = emptyList(),
        val isSearchingUsers: Boolean = false,
        val isLoading: Boolean = true,
        val error: AppError? = null,
    ) : UiState {
        val filteredUsers: List<SocialUser>
            get() {
                val q = query.trim().lowercase()
                if (q.isBlank()) return users
                return users.filter {
                    it.displayName.lowercase().contains(q) || it.username.lowercase().contains(q)
                }
            }

        val filteredArtists: List<Artist>
            get() {
                val q = query.trim().lowercase()
                if (q.isBlank()) return artists
                return artists.filter { it.name.lowercase().contains(q) }
            }
    }

    sealed interface Event : UiEvent {
        data class Load(val userId: Int) : Event
        data class QueryChanged(val query: String) : Event
    }

    sealed interface Effect : UiEffect
}
