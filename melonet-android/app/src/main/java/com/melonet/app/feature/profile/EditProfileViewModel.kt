package com.melonet.app.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.core.common.Result
import com.melonet.app.data.repository.AuthRepository
import com.melonet.app.data.repository.UserRepository
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val appContext: Context,
) : BaseViewModel<EditProfileContract.State, EditProfileContract.Event, EditProfileContract.Effect>() {

    override fun createInitialState() = EditProfileContract.State()

    init {
        handleEvent(EditProfileContract.Event.Load)
    }

    override fun handleEvent(event: EditProfileContract.Event) {
        when (event) {
            EditProfileContract.Event.Load -> loadProfile()
            is EditProfileContract.Event.DisplayNameChanged -> setState { copy(displayName = event.value) }
            is EditProfileContract.Event.BioChanged -> setState { copy(bio = event.value) }
            is EditProfileContract.Event.EmailChanged -> setState { copy(email = event.value) }
            is EditProfileContract.Event.AvatarPicked -> uploadAvatar(event.uri)
            EditProfileContract.Event.SaveClicked -> saveProfile()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            when (val result = userRepository.getCurrentUser()) {
                is Result.Success -> setState {
                    copy(
                        displayName = result.data.displayName,
                        bio = result.data.bio,
                        email = result.data.email,
                        avatarUrl = result.data.avatarUrl,
                    )
                }
                is Result.Error -> setEffect {
                    EditProfileContract.Effect.ShowError(result.error.toMessage())
                }
            }
        }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            setState { copy(isSaving = true, error = null) }
            when (
                val result = userRepository.updateProfile(
                    displayName = uiState.value.displayName,
                    bio = uiState.value.bio,
                    email = uiState.value.email,
                )
            ) {
                is Result.Success -> {
                    authRepository.applyUser(result.data)
                    setState { copy(isSaving = false) }
                    setEffect { EditProfileContract.Effect.NavigateBack }
                }
                is Result.Error -> {
                    setState { copy(isSaving = false) }
                    setEffect { EditProfileContract.Effect.ShowError(result.error.toMessage()) }
                }
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            setState { copy(isUploadingAvatar = true) }
            val contentResolver = appContext.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                setState { copy(isUploadingAvatar = false) }
                return@launch
            }
            val part = MultipartBody.Part.createFormData(
                "avatar",
                "avatar.jpg",
                bytes.toRequestBody(mimeType.toMediaTypeOrNull()),
            )
            when (val result = userRepository.uploadAvatar(part)) {
                is Result.Success -> {
                    authRepository.applyUser(result.data)
                    setState { copy(isUploadingAvatar = false, avatarUrl = result.data.avatarUrl) }
                }
                is Result.Error -> {
                    setState { copy(isUploadingAvatar = false) }
                    setEffect { EditProfileContract.Effect.ShowError(result.error.toMessage()) }
                }
            }
        }
    }

    private fun AppError.toMessage(): String = when (this) {
        is AppError.Network -> message
        is AppError.Unknown -> message
        AppError.Unauthorized -> "Unauthorized"
    }
}
