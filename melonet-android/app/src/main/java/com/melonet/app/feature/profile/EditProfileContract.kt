package com.melonet.app.feature.profile

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState

object EditProfileContract {
    data class State(
        val displayName: String = "",
        val bio: String = "",
        val email: String = "",
        val avatarUrl: String = "",
        val isSaving: Boolean = false,
        val isUploadingAvatar: Boolean = false,
        val error: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object Load : Event
        data class DisplayNameChanged(val value: String) : Event
        data class BioChanged(val value: String) : Event
        data class EmailChanged(val value: String) : Event
        data class AvatarPicked(val uri: android.net.Uri) : Event
        data object SaveClicked : Event
    }

    sealed interface Effect : UiEffect {
        data object NavigateBack : Effect
        data class ShowError(val message: String) : Effect
    }
}
