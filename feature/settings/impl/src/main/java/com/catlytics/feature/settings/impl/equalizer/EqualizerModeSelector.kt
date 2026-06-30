package com.catlytics.feature.settings.impl.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catlytics.core.model.EqualizerMode

@Composable
internal fun EqualizerModeSelector(
    currentMode: EqualizerMode,
    onModeSelected: (EqualizerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.64f),
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
                shape = shape,
            )
            .padding(4.dp),
    ) {
        EqualizerModeTab(
            text = "Predefinido",
            selected = currentMode == EqualizerMode.Preset,
            onClick = { onModeSelected(EqualizerMode.Preset) },
            modifier = Modifier.weight(1f),
        )
        EqualizerModeTab(
            text = "Personalizado",
            selected = currentMode == EqualizerMode.Custom,
            onClick = { onModeSelected(EqualizerMode.Custom) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EqualizerModeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
