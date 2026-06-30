package com.catlytics.feature.settings.impl.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.catlytics.core.model.EqualizerState

@Composable
internal fun EqualizerGraph(
    equalizerState: EqualizerState,
    onCustomBandLevelChange: (Short, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = equalizerState.enabled && equalizerState.isAvailable
    val isCustom = equalizerState.mode == com.catlytics.core.model.EqualizerMode.Custom && active
    val primary = MaterialTheme.colorScheme.primary
    val barColor = if (active) primary else MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)

    val localLevels = remember(equalizerState.bands) { mutableStateMapOf<Short, Int>() }

    Canvas(
        modifier = modifier
            .height(220.dp)
            .pointerInput(isCustom, equalizerState.bands) {
                val range = equalizerState.levelRange
                if (!isCustom || equalizerState.bands.isEmpty() || range == null) return@pointerInput
                val bandsCount = equalizerState.bands.size

                fun updateBand(offset: Offset, isFinal: Boolean) {
                    val slotWidth = size.width / bandsCount
                    val index = (offset.x / slotWidth).toInt().coerceIn(0, bandsCount - 1)
                    val band = equalizerState.bands[index]
                    val levelFraction = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    val newLevel = (range.minMilliBel + levelFraction * (range.maxMilliBel - range.minMilliBel)).toInt()

                    localLevels[band.id] = newLevel
                    onCustomBandLevelChange(band.id, newLevel, isFinal)
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    updateBand(down.position, isFinal = false)
                    down.consume()

                    var lastPosition = down.position

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            lastPosition = change.position
                            updateBand(change.position, isFinal = false)
                            change.consume()
                        } else {
                            updateBand(lastPosition, isFinal = true)
                            break
                        }
                    }
                }
            },
    ) {
        repeat(5) { index ->
            val y = size.height * index / 4f
            drawLine(
                color = gridColor,
                start = Offset(x = 0f, y = y),
                end = Offset(x = size.width, y = y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val slotWidth = size.width / equalizerState.bands.size.coerceAtLeast(1)
        val barWidth = slotWidth * 0.22f

        equalizerState.bands.forEachIndexed { index, band ->
            val actualLevel = localLevels[band.id] ?: band.levelMilliBel
            val min = equalizerState.levelRange?.minMilliBel ?: -1500
            val max = equalizerState.levelRange?.maxMilliBel ?: 1500
            val span = (max - min).coerceAtLeast(1)
            val displayValue = ((actualLevel - min).toFloat() / span).coerceIn(0.12f, 1f)

            val barHeight = size.height * displayValue
            val left = slotWidth * index + (slotWidth - barWidth) / 2f
            val top = size.height - barHeight
            drawRoundRect(
                color = barColor.copy(alpha = if (active) 0.92f else 0.38f),
                topLeft = Offset(x = left, y = top),
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = CornerRadius(x = barWidth / 2f, y = barWidth / 2f),
            )
            if (active) {
                drawRoundRect(
                    color = primary.copy(alpha = 0.22f),
                    topLeft = Offset(x = left - barWidth * 0.5f, y = top - 10.dp.toPx()),
                    size = Size(width = barWidth * 2f, height = barHeight + 20.dp.toPx()),
                    cornerRadius = CornerRadius(x = barWidth, y = barWidth),
                )
            }
            if (isCustom) {
                val circleRadius = barWidth * 1.5f
                val centerX = left + barWidth / 2f

                drawCircle(
                    color = primary,
                    radius = circleRadius,
                    center = Offset(x = centerX, y = top)
                )
                drawCircle(
                    color = Color.White,
                    radius = circleRadius * 0.4f,
                    center = Offset(x = centerX, y = top)
                )
            }
        }
    }
}

@Composable
internal fun EqualizerFrequencyScale(
    equalizerState: EqualizerState,
    modifier: Modifier = Modifier,
) {
    val labels = equalizerState.frequencyLabels
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
        }
    }
}

// UI formatting extensions local to equalizer UI
private val EqualizerState.frequencyLabels: List<String>
    get() {
        val frequencies = bands.map { it.centerFrequencyHz }.takeIf { it.isNotEmpty() }
            ?: listOf(60, 230, 910, 3_600, 14_000)
        return frequencies.map { frequency ->
            if (frequency >= 1_000) {
                "${frequency / 1_000}K"
            } else {
                frequency.toString()
            }
        }
    }
