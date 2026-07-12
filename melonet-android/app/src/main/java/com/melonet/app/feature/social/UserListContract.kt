package com.melonet.app.feature.social

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.model.SocialUser
import com.melonet.app.data.model.UserListType

object UserListContract {
    data class State(
        val listType: UserListType = UserListType.FOLLOWING,
        val users: List<SocialUser> = emptyList(),
        val isLoading: Boolean = true,
        val error: AppError? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val userId: Int, val listType: UserListType) : Event
        data class UserClicked(val userId: Int) : Event
    }

    sealed interface Effect : UiEffect {
        data class NavigateToUser(val userId: Int) : Effect
    }
}
