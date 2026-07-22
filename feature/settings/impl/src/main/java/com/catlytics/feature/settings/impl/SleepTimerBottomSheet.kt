package com.catlytics.feature.settings.impl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.catlytics.core.model.SleepTimerState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepTimerBottomSheet(
    state: SleepTimerState,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val activeState = state as? SleepTimerState.Active
    val initialMinutes = activeState
        ?.totalDurationMillis
        ?.div(MILLIS_PER_MINUTE)
        ?.toInt()
        ?.let(::snapSleepTimerMinutes)
        ?: DEFAULT_SLEEP_TIMER_MINUTES
    var selectedMinutes by rememberSaveable(activeState?.totalDurationMillis) {
        mutableIntStateOf(initialMinutes)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Temporizador de sueño",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "La música se pausará sin perder tu cola ni tu posición.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            activeState?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Temporizador activo",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formatSleepTimerRemaining(it.remainingMillis),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            SleepTimerDial(
                selectedMinutes = selectedMinutes,
                onMinutesChange = { selectedMinutes = it },
            )

            Button(
                onClick = { onStart(selectedMinutes) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (activeState == null) "Iniciar temporizador" else "Reiniciar temporizador")
            }

            if (activeState != null) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancelar temporizador")
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDial(
    selectedMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedSweepDegrees by animateFloatAsState(
        targetValue = selectedMinutes.toFloat() / MAX_SLEEP_TIMER_MINUTES * FULL_CIRCLE_DEGREES,
        label = "Sleep timer dial",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val progressColor = MaterialTheme.colorScheme.primary
    val knobColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(264.dp)
            .testTag(SLEEP_TIMER_DIAL_TEST_TAG)
            .semantics {
                stateDescription = "$selectedMinutes minutos"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = selectedMinutes.toFloat(),
                    range = MIN_SLEEP_TIMER_MINUTES.toFloat()..MAX_SLEEP_TIMER_MINUTES.toFloat(),
                    steps = SLEEP_TIMER_OPTION_COUNT - 2,
                )
                setProgress { targetValue ->
                    onMinutesChange(snapSleepTimerMinutes(targetValue.roundToInt()))
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .padding(18.dp)
                .pointerInput(onMinutesChange) {
                    detectDragGestures(
                        onDragStart = { position ->
                            onMinutesChange(minutesForDialPosition(position, size.toSize()))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            onMinutesChange(minutesForDialPosition(change.position, size.toSize()))
                        },
                    )
                },
        ) {
            val strokeWidth = 14.dp.toPx()
            val dialInset = strokeWidth / 2f
            val dialSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val dialTopLeft = Offset(dialInset, dialInset)

            drawArc(
                color = trackColor,
                startAngle = DIAL_START_ANGLE,
                sweepAngle = FULL_CIRCLE_DEGREES,
                useCenter = false,
                topLeft = dialTopLeft,
                size = dialSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = DIAL_START_ANGLE,
                sweepAngle = animatedSweepDegrees,
                useCenter = false,
                topLeft = dialTopLeft,
                size = dialSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            val knobAngleRadians = (animatedSweepDegrees + DIAL_START_ANGLE) * PI / 180.0
            val radius = dialSize.width / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val knobCenter = Offset(
                x = center.x + radius * cos(knobAngleRadians).toFloat(),
                y = center.y + radius * sin(knobAngleRadians).toFloat(),
            )
            drawCircle(
                color = knobColor,
                radius = 11.dp.toPx(),
                center = knobCenter,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = selectedMinutes.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "min",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                text = "Desliza para ajustar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun minutesForDialPosition(position: Offset, size: Size): Int {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val angleFromTop = (
        Math.toDegrees(
            atan2(
                (position.y - centerY).toDouble(),
                (position.x - centerX).toDouble(),
            ),
        ) + 90.0 + FULL_CIRCLE_DEGREES
        ) % FULL_CIRCLE_DEGREES
    val rawMinutes = angleFromTop / FULL_CIRCLE_DEGREES * MAX_SLEEP_TIMER_MINUTES
    return snapSleepTimerMinutes(rawMinutes.roundToInt())
}

internal fun snapSleepTimerMinutes(minutes: Int): Int {
    val snappedMinutes = (minutes.toFloat() / SLEEP_TIMER_STEP_MINUTES).roundToInt() *
        SLEEP_TIMER_STEP_MINUTES
    return snappedMinutes.coerceIn(MIN_SLEEP_TIMER_MINUTES, MAX_SLEEP_TIMER_MINUTES)
}

internal fun formatSleepTimerRemaining(remainingMillis: Long): String {
    val totalSeconds = ceil(remainingMillis.coerceAtLeast(0L) / 1_000.0).toLong()
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun androidx.compose.ui.unit.IntSize.toSize() = Size(width.toFloat(), height.toFloat())

internal const val MIN_SLEEP_TIMER_MINUTES = 5
internal const val MAX_SLEEP_TIMER_MINUTES = 120
internal const val SLEEP_TIMER_STEP_MINUTES = 5
internal const val SLEEP_TIMER_DIAL_TEST_TAG = "sleep_timer_dial"
private const val DEFAULT_SLEEP_TIMER_MINUTES = 30
private const val MILLIS_PER_MINUTE = 60_000L
private const val FULL_CIRCLE_DEGREES = 360f
private const val DIAL_START_ANGLE = -90f
private const val SLEEP_TIMER_OPTION_COUNT =
    (MAX_SLEEP_TIMER_MINUTES - MIN_SLEEP_TIMER_MINUTES) / SLEEP_TIMER_STEP_MINUTES + 1
