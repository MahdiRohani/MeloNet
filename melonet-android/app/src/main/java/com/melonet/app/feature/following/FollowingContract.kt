package com.melonet.app.feature.following

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.Artist
import com.melonet.app.data.model.SocialUser

object FollowingContract {

    data class State(
        val users: List<SocialUser> = emptyList(),
        val artists: List<Artist> = emptyList(),
        val isLoading: Boolean = true,
        val error: AppError? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val userId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
