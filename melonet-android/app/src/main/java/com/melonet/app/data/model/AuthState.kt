package com.melonet.app.data.model

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(
        val user: User,
        val isOfflineSession: Boolean = false,
    ) : AuthState
}
