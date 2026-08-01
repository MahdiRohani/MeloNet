package com.melonet.app.feature.equalizer

import com.melonet.app.core.common.UiEffect
import com.melonet.app.core.common.UiEvent
import com.melonet.app.core.common.UiState
import com.melonet.app.data.local.EqualizerSettings

object EqualizerContract {
    data class State(
        val settings: EqualizerSettings = EqualizerSettings(),
        val bandCount: Int = 5,
        val minLevel: Int = -1500,
        val maxLevel: Int = 1500,
        val centerFreqsHz: List<Int> = emptyList(),
        val presetNames: List<String> = listOf(
            "Normal", "Bass Boost", "Treble", "Rock", "Pop", "Jazz", "Classical",
        ),
    ) : UiState

    sealed interface Event : UiEvent {
        data object Load : Event
        data class PresetSelected(val name: String) : Event
        data class BandChanged(val index: Int, val levelMilliBel: Int) : Event
        data class BassBoostChanged(val strength: Int) : Event
        data class VirtualizerChanged(val strength: Int) : Event
        data class EnabledChanged(val enabled: Boolean) : Event
        data object Reset : Event
    }

    sealed interface Effect : UiEffect {
        data object NavigateBack : Effect
    }
}
