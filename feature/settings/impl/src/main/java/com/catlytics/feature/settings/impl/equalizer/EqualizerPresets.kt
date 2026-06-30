package com.catlytics.feature.settings.impl.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState

@Composable
internal fun EqualizerPresetDropdown(
    equalizerState: EqualizerState,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onPresetSelected: (EqualizerPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EqualizerPresetSelectorRow(
            equalizerState = equalizerState,
            expanded = expanded,
            onClick = onExpandedChange,
        )
        if (expanded) {
            if (equalizerState.presets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.64f),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(16.dp),
                ) {
                    Text(
                        text = equalizerState.errorMessage
                            ?: "El dispositivo no expone presets para esta sesión.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    equalizerState.presets.forEach { preset ->
                        EqualizerPresetOption(
                            preset = preset,
                            selected = preset.name == equalizerState.selectedPresetName,
                            onClick = { onPresetSelected(preset) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EqualizerPresetSelectorRow(
    equalizerState: EqualizerState,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (expanded) 0.72f else 0.42f),
                shape = shape,
            )
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (expanded) 0.14f else 0.08f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            EqualizerSectionLabel(text = "DEVICE PRESET")
            Text(
                text = equalizerState.selectedPresetName ?: "Seleccionar curva",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        EqualizerTechPill(
            text = if (expanded) "CLOSE" else "${equalizerState.presets.size} PRESETS",
            active = equalizerState.presets.isNotEmpty(),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_down),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun EqualizerPresetOption(
    preset: EqualizerPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                },
                shape = shape,
            )
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.64f)
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(30.dp)
                .background(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.44f)
                    },
                    shape = RoundedCornerShape(12.dp),
                ),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "PRESET ${preset.id.toInt().plus(1).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        EqualizerTechPill(
            text = if (selected) "ACTIVE" else "LOAD",
            active = selected,
        )
    }
}
