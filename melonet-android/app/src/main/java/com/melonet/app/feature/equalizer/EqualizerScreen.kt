package com.melonet.app.feature.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloFilterChip
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing
    val settings = state.settings
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.primary.copy(alpha = 0.22f),
                        scheme.background,
                        scheme.background,
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background.copy(alpha = 0f),
                ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                EqualizerSectionCard {
                    Text(
                        text = stringResource(R.string.equalizer_presets),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(spacing.sm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        state.presetNames.forEach { name ->
                            MeloFilterChip(
                                label = equalizerPresetLabel(name),
                                selected = settings.selectedPresetName.equals(name, ignoreCase = true),
                                onClick = {
                                    viewModel.handleEvent(EqualizerContract.Event.PresetSelected(name))
                                },
                            )
                        }
                    }
                }

                EqualizerSectionCard {
                    Text(
                        text = stringResource(R.string.equalizer_bands),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(spacing.md))
                    val bandCount = state.bandCount.coerceAtLeast(1)
                    val levels = List(bandCount) { index ->
                        settings.bandLevelsMilliBel.getOrNull(index) ?: 0
                    }
                    levels.forEachIndexed { index, level ->
                        val freqHz = state.centerFreqsHz.getOrNull(index)
                        BandSliderRow(
                            title = equalizerBandTitle(index, bandCount, freqHz),
                            levelMilliBel = level,
                            valueRange = state.minLevel.toFloat()..state.maxLevel.toFloat(),
                            enabled = settings.enabled,
                            onLevelChange = { milliBel ->
                                viewModel.handleEvent(
                                    EqualizerContract.Event.BandChanged(index, milliBel),
                                )
                            },
                        )
                        if (index < levels.lastIndex) {
                            Spacer(modifier = Modifier.height(spacing.md))
                        }
                    }
                }

                EqualizerSectionCard {
                    StrengthSliderRow(
                        title = stringResource(R.string.equalizer_bass_boost),
                        strength = settings.bassBoostStrength,
                        enabled = settings.enabled,
                        onStrengthChange = {
                            viewModel.handleEvent(EqualizerContract.Event.BassBoostChanged(it))
                        },
                    )
                    Spacer(modifier = Modifier.height(spacing.lg))
                    StrengthSliderRow(
                        title = stringResource(R.string.equalizer_virtualizer),
                        strength = settings.virtualizerStrength,
                        enabled = settings.enabled,
                        onStrengthChange = {
                            viewModel.handleEvent(EqualizerContract.Event.VirtualizerChanged(it))
                        },
                    )
                }

                MeloButton(
                    text = stringResource(R.string.equalizer_reset),
                    onClick = { viewModel.handleEvent(EqualizerContract.Event.Reset) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(spacing.sm))
            }
        }
    }
}

@Composable
private fun EqualizerSectionCard(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val spacing = MeloNetTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.surfaceVariant.copy(alpha = 0.55f),
                        scheme.surface.copy(alpha = 0.72f),
                    ),
                ),
            )
            .padding(spacing.md),
    ) {
        content()
    }
}

@Composable
private fun BandSliderRow(
    title: String,
    levelMilliBel: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatDbLabel(levelMilliBel),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        EqualizerSlider(
            value = levelMilliBel.toFloat(),
            onValueChange = { onLevelChange(it.toInt()) },
            valueRange = valueRange,
            enabled = enabled,
            activeColor = scheme.primary,
            trackColor = scheme.onSurface.copy(alpha = 0.18f),
            thumbColor = scheme.primary,
        )
    }
}

@Composable
private fun StrengthSliderRow(
    title: String,
    strength: Int,
    enabled: Boolean,
    onStrengthChange: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val percent = ((strength.coerceIn(0, 1000) / 1000f) * 100f).toInt()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.equalizer_percent, percent),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        EqualizerSlider(
            value = strength.toFloat(),
            onValueChange = { onStrengthChange(it.toInt()) },
            valueRange = 0f..1000f,
            enabled = enabled,
            activeColor = scheme.secondary,
            trackColor = scheme.onSurface.copy(alpha = 0.18f),
            thumbColor = scheme.secondary,
        )
    }
}

@Composable
private fun equalizerPresetLabel(name: String): String =
    when (name.lowercase()) {
        "normal" -> stringResource(R.string.equalizer_preset_normal)
        "bass boost", "bass" -> stringResource(R.string.equalizer_preset_bass_boost)
        "treble", "treble boost" -> stringResource(R.string.equalizer_preset_treble)
        "rock" -> stringResource(R.string.equalizer_preset_rock)
        "pop" -> stringResource(R.string.equalizer_preset_pop)
        "jazz" -> stringResource(R.string.equalizer_preset_jazz)
        "classical" -> stringResource(R.string.equalizer_preset_classical)
        "custom" -> stringResource(R.string.equalizer_preset_custom)
        else -> name
    }

@Composable
private fun equalizerBandTitle(index: Int, bandCount: Int, freqHz: Int?): String {
    val freqLabel = when {
        freqHz == null || freqHz <= 0 -> stringResource(R.string.equalizer_band_index, index + 1)
        freqHz >= 1000 -> {
            val khz = freqHz / 1000f
            if (abs(khz - khz.toInt()) < 0.05f) {
                stringResource(R.string.equalizer_band_khz_int, khz.toInt())
            } else {
                stringResource(R.string.equalizer_band_khz, khz)
            }
        }
        else -> stringResource(R.string.equalizer_band_hz, freqHz)
    }
    if (freqHz == null || freqHz <= 0) return freqLabel

    val roleRes = when {
        bandCount <= 1 -> null
        index == 0 -> R.string.equalizer_band_bass
        index == bandCount - 1 -> R.string.equalizer_band_treble
        index == bandCount / 2 -> R.string.equalizer_band_mid
        index < bandCount / 2 -> R.string.equalizer_band_low_mid
        else -> R.string.equalizer_band_high_mid
    }
    return if (roleRes != null) stringResource(roleRes, freqLabel) else freqLabel
}

@Composable
private fun formatDbLabel(milliBel: Int): String {
    val db = milliBel / 100f
    return when {
        db > 0.049f -> stringResource(R.string.equalizer_db_positive, db)
        db < -0.049f -> stringResource(R.string.equalizer_db_negative, abs(db))
        else -> stringResource(R.string.equalizer_db_zero)
    }
}
