package com.melonet.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melonet.app.data.model.AuthState
import com.melonet.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    init {
        restoreSession()
    }

    fun restoreSession() {
        viewModelScope.launch { authRepository.restoreSession() }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
