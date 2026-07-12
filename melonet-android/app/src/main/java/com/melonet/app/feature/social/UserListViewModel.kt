package com.melonet.app.feature.social

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.model.UserListType
import com.melonet.app.data.repository.SocialRepository
import kotlinx.coroutines.launch

class UserListViewModel(
    private val socialRepository: SocialRepository,
) : BaseViewModel<UserListContract.State, UserListContract.Event, UserListContract.Effect>() {

    override fun createInitialState() = UserListContract.State()

    override fun handleEvent(event: UserListContract.Event) {
        when (event) {
            is UserListContract.Event.Load -> load(event.userId, event.listType)
            is UserListContract.Event.UserClicked -> {
                setEffect { UserListContract.Effect.NavigateToUser(event.userId) }
            }
        }
    }

    private fun load(userId: Int, listType: UserListType) {
        viewModelScope.launch {
            setState { copy(isLoading = true, listType = listType, error = null) }
            when (val result = socialRepository.getUserList(userId, listType)) {
                is Result.Success -> setState {
                    copy(isLoading = false, users = result.data, error = null)
                }
                is Result.Error -> setState { copy(isLoading = false, error = result.error) }
            }
        }
    }
}
