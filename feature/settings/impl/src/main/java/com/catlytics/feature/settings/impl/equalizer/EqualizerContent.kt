package com.catlytics.feature.settings.impl.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState

@Composable
internal fun EqualizerSettingsContent(
    equalizerState: EqualizerState,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    onEqualizerModeChange: (EqualizerMode) -> Unit,
    onEqualizerPresetSelected: (EqualizerPreset) -> Unit,
    onCustomBandLevelChange: (Short, Int, Boolean) -> Unit,
    bottomPadding: () -> Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 24.dp,
            end = 20.dp,
            bottom = bottomPadding() + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            EqualizerVisualizerPanel(
                equalizerState = equalizerState,
                onEqualizerEnabledChange = onEqualizerEnabledChange,
                onEqualizerModeChange = onEqualizerModeChange,
                onEqualizerPresetSelected = onEqualizerPresetSelected,
                onCustomBandLevelChange = onCustomBandLevelChange,
            )
        }
    }
}

@Composable
private fun EqualizerVisualizerPanel(
    equalizerState: EqualizerState,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    onEqualizerModeChange: (EqualizerMode) -> Unit,
    onEqualizerPresetSelected: (EqualizerPreset) -> Unit,
    onCustomBandLevelChange: (Short, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = equalizerState.enabled && equalizerState.isAvailable
    var presetsExpanded by rememberSaveable { mutableStateOf(false) }

    EqualizerGlassPanel(
        highlighted = active,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            EqualizerModeSelector(
                currentMode = equalizerState.mode,
                onModeSelected = onEqualizerModeChange,
            )
            if (equalizerState.mode == EqualizerMode.Preset) {
                EqualizerPresetDropdown(
                    equalizerState = equalizerState,
                    expanded = presetsExpanded,
                    onExpandedChange = { presetsExpanded = !presetsExpanded },
                    onPresetSelected = { preset ->
                        onEqualizerPresetSelected(preset)
                        presetsExpanded = false
                    },
                )
            }
            EqualizerGraph(
                equalizerState = equalizerState,
                onCustomBandLevelChange = onCustomBandLevelChange,
                modifier = Modifier.fillMaxWidth(),
            )
            EqualizerFrequencyScale(equalizerState = equalizerState)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            EqualizerOutputRow(
                equalizerState = equalizerState,
                onEqualizerEnabledChange = onEqualizerEnabledChange,
            )
        }
    }
}
