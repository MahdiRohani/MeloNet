package com.melonet.app.feature.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloFilterChip
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val settings = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.equalizer_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_back),
                    )
                }
            },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.equalizer_enabled),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = spacing.sm),
                    )
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = {
                            viewModel.handleEvent(EqualizerContract.Event.EnabledChanged(it))
                        },
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.equalizer_presets),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                state.presetNames.forEach { name ->
                    MeloFilterChip(
                        label = name,
                        selected = settings.selectedPresetName.equals(name, ignoreCase = true),
                        onClick = { viewModel.handleEvent(EqualizerContract.Event.PresetSelected(name)) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.equalizer_bands),
                style = MaterialTheme.typography.titleMedium,
            )
            val bandCount = state.bandCount.coerceAtLeast(1)
            val levels = List(bandCount) { index ->
                settings.bandLevelsMilliBel.getOrNull(index) ?: 0
            }
            levels.forEachIndexed { index, level ->
                val label = state.centerFreqsHz.getOrNull(index)?.let { freq ->
                    if (freq >= 1000) stringResource(R.string.equalizer_band_khz, freq / 1000f)
                    else stringResource(R.string.equalizer_band_hz, freq)
                } ?: stringResource(R.string.equalizer_band_index, index + 1)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = level.toFloat(),
                        onValueChange = {
                            viewModel.handleEvent(
                                EqualizerContract.Event.BandChanged(index, it.toInt()),
                            )
                        },
                        valueRange = state.minLevel.toFloat()..state.maxLevel.toFloat(),
                        enabled = settings.enabled,
                    )
                }
            }

            Text(
                text = stringResource(R.string.equalizer_bass_boost),
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = settings.bassBoostStrength.toFloat(),
                onValueChange = {
                    viewModel.handleEvent(EqualizerContract.Event.BassBoostChanged(it.toInt()))
                },
                valueRange = 0f..1000f,
                enabled = settings.enabled,
            )

            Text(
                text = stringResource(R.string.equalizer_virtualizer),
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = settings.virtualizerStrength.toFloat(),
                onValueChange = {
                    viewModel.handleEvent(EqualizerContract.Event.VirtualizerChanged(it.toInt()))
                },
                valueRange = 0f..1000f,
                enabled = settings.enabled,
            )

            Spacer(modifier = Modifier.height(spacing.sm))
            MeloButton(
                text = stringResource(R.string.equalizer_reset),
                onClick = { viewModel.handleEvent(EqualizerContract.Event.Reset) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
