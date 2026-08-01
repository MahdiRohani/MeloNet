package com.melonet.app.feature.equalizer

import androidx.lifecycle.viewModelScope
import com.melonet.app.core.common.BaseViewModel
import com.melonet.app.data.local.EqualizerSettings
import com.melonet.app.data.local.SettingsRepository
import com.melonet.app.feature.player.EqualizerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class EqualizerViewModel(
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<EqualizerContract.State, EqualizerContract.Event, EqualizerContract.Effect>() {

    private var persistJob: Job? = null

    override fun createInitialState() = EqualizerContract.State()

    init {
        handleEvent(EqualizerContract.Event.Load)
        settingsRepository.equalizerSettingsFlow
            .onEach { settings ->
                // Ignore DataStore echoes while a local optimistic edit is pending.
                if (persistJob?.isActive == true) return@onEach
                EqualizerController.updateSettings(settings)
                setState { copy(settings = settings) }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: EqualizerContract.Event) {
        when (event) {
            EqualizerContract.Event.Load -> refreshHardwareInfo()
            is EqualizerContract.Event.PresetSelected -> selectPreset(event.name)
            is EqualizerContract.Event.BandChanged -> updateBand(event.index, event.levelMilliBel)
            is EqualizerContract.Event.BassBoostChanged -> updateBass(event.strength)
            is EqualizerContract.Event.VirtualizerChanged -> updateVirtualizer(event.strength)
            is EqualizerContract.Event.EnabledChanged -> updateEnabled(event.enabled)
            EqualizerContract.Event.Reset -> selectPreset("Normal")
        }
    }

    private fun refreshHardwareInfo() {
        val bandCount = EqualizerController.bandCount().takeIf { it > 0 } ?: 5
        val range = EqualizerController.bandLevelRange()
        val freqs = List(bandCount) { EqualizerController.centerFreqHz(it) }
        val hwPresets = EqualizerController.builtInPresetNames()
        setState {
            copy(
                bandCount = bandCount,
                minLevel = range[0].toInt(),
                maxLevel = range[1].toInt(),
                centerFreqsHz = freqs,
                presetNames = (listOf("Normal", "Bass Boost", "Treble", "Rock", "Pop", "Jazz", "Classical") + hwPresets)
                    .distinct(),
            )
        }
    }

    private fun selectPreset(name: String) {
        EqualizerController.applyNamedPreset(name)
        val next = EqualizerController.currentSettings().copy(selectedPresetName = name)
        applyOptimistic(next, persistImmediately = true)
        refreshHardwareInfo()
    }

    private fun updateBand(index: Int, level: Int) {
        val current = uiState.value.settings
        val bands = current.bandLevelsMilliBel
            .toMutableList()
            .also { list ->
                while (list.size <= index) list.add(0)
                list[index] = level
            }
        val next = current.copy(
            usePreset = false,
            selectedPresetName = "Custom",
            bandLevelsMilliBel = bands,
            enabled = true,
        )
        applyOptimistic(next)
    }

    private fun updateBass(strength: Int) {
        val next = uiState.value.settings.copy(bassBoostStrength = strength.coerceIn(0, 1000))
        applyOptimistic(next)
    }

    private fun updateVirtualizer(strength: Int) {
        val next = uiState.value.settings.copy(virtualizerStrength = strength.coerceIn(0, 1000))
        applyOptimistic(next)
    }

    private fun updateEnabled(enabled: Boolean) {
        val next = uiState.value.settings.copy(enabled = enabled)
        applyOptimistic(next, persistImmediately = true)
    }

    /** UI + hardware immediately; DataStore debounced unless [persistImmediately]. */
    private fun applyOptimistic(next: EqualizerSettings, persistImmediately: Boolean = false) {
        setState { copy(settings = next) }
        EqualizerController.updateSettings(next)
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            if (!persistImmediately) delay(PERSIST_DEBOUNCE_MS)
            settingsRepository.saveEqualizerSettings(next)
        }
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 400L
    }
}
