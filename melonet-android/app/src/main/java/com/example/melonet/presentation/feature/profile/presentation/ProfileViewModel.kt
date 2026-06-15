package com.example.melonet.presentation.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melonet.presentation.feature.profile.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ProfileState(
    val userName: String = "کاربر تستی",
    val avatarUrl: String = "",
    val isPremium: Boolean = false,
    val isLoading: Boolean = false
)

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        observePremiumStatus()
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            userRepository.isPremiumFlow.collect { isPremiumStatus ->
                _state.update { it.copy(isPremium = isPremiumStatus) }
            }
        }
    }

    fun onUpgradePremiumClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            userRepository.upgradeToPremium()
            _state.update { it.copy(isLoading = false) }
        }
    }
}