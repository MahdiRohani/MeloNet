package com.melonet.app.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<State : UiState, Event : UiEvent, Effect : UiEffect> : ViewModel() {

    abstract fun createInitialState(): State

    private val initialState: State by lazy { createInitialState() }

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    abstract fun handleEvent(event: Event)

    protected fun setState(reduce: State.() -> State) {
        _uiState.value = _uiState.value.reduce()
    }

    protected fun setEffect(builder: () -> Effect) {
        viewModelScope.launch { _effect.send(builder()) }
    }
}
