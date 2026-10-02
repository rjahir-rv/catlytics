package com.catlytics.app.playback

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.format.SleepTimerFormat
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.Track

/** Barra fija al fondo del modal de la fila: aleatorio, repetir y temporizador (estilo Spotify). */
@Composable
internal fun QueueControlsBar(
    isShuffleEnabled: Boolean,
    repeatMode: PlaybackRepeatMode,
    sleepTimerState: SleepTimerState,
    accent: Color,
    enabled: Boolean,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var showTimerPresets by rememberSaveable { mutableStateOf(false) }
    val activeTimer = sleepTimerState as? SleepTimerState.Active

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = showTimerPresets,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            SleepTimerPresets(
                activeTimer = activeTimer,
                accent = accent,
                onSelect = { minutes ->
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onStartSleepTimer(minutes)
                    showTimerPresets = false
                },
                onCancel = {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    onCancelSleepTimer()
                    showTimerPresets = false
                },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueToggleChip(
                iconRes = R.drawable.ic_shuffle,
                labelKey = isShuffleEnabled,
                label = stringResource(AppR.string.app_queue_shuffle),
                stateDescription = if (isShuffleEnabled) {
                    stringResource(AppR.string.app_state_on)
                } else {
                    stringResource(AppR.string.app_state_off)
                },
                active = isShuffleEnabled,
                enabled = enabled,
                accent = accent,
                onClick = {
                    haptics.performHapticFeedback(
                        if (isShuffleEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                    )
                    onToggleShuffle()
                },
            )
            QueueToggleChip(
                iconRes = if (repeatMode == PlaybackRepeatMode.One) {
                    R.drawable.ic_repeat_one
                } else {
                    R.drawable.ic_repeat_round
                },
                labelKey = repeatMode,
                label = when (repeatMode) {
                    PlaybackRepeatMode.Off -> stringResource(AppR.string.app_queue_repeat)
                    PlaybackRepeatMode.One -> stringResource(AppR.string.app_queue_repeat_one)
                    PlaybackRepeatMode.All -> stringResource(AppR.string.app_queue_repeat_all)
                },
                stateDescription = when (repeatMode) {
                    PlaybackRepeatMode.Off -> stringResource(AppR.string.app_repeat_state_off)
                    PlaybackRepeatMode.One -> stringResource(AppR.string.app_repeat_state_one)
                    PlaybackRepeatMode.All -> stringResource(AppR.string.app_repeat_state_all)
                },
                active = repeatMode != PlaybackRepeatMode.Off,
                enabled = enabled,
                accent = accent,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onCycleRepeatMode()
                },
            )
            val timerRemaining = activeTimer?.let { SleepTimerFormat.formatRemaining(it.remainingMillis) }
            QueueToggleChip(
                iconRes = R.drawable.ic_timer,
                labelKey = activeTimer != null,
                label = timerRemaining ?: stringResource(AppR.string.app_queue_sleep_timer),
                stateDescription = timerRemaining?.let {
                    stringResource(AppR.string.app_queue_sleep_timer_active, it)
                } ?: stringResource(AppR.string.app_state_off),
                active = activeTimer != null || showTimerPresets,
                enabled = true,
                accent = accent,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showTimerPresets = !showTimerPresets
                },
            )
        }
    }
}

@Composable
private fun SleepTimerPresets(
    activeTimer: SleepTimerState.Active?,
    accent: Color,
    onSelect: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val activeMinutes = activeTimer?.let { (it.totalDurationMillis / MILLIS_PER_MINUTE).toInt() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        SLEEP_TIMER_PRESETS_MINUTES.forEach { minutes ->
            PresetPill(
                label = stringResource(AppR.string.app_queue_sleep_timer_minutes, minutes),
                selected = minutes == activeMinutes,
                accent = accent,
                onClick = { onSelect(minutes) },
            )
        }
        if (activeTimer != null) {
            CancelTimerPill(onClick = onCancel)
        }
    }
}

@Composable
private fun PresetPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        label = "PresetPillContainer",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.pressScale(interactionSource, pressedScale = 0.94f),
        shape = CircleShape,
        color = container,
        contentColor = if (selected) accent.readableContentColor() else MaterialTheme.colorScheme.onSurface,
        interactionSource = interactionSource,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun CancelTimerPill(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = Modifier.pressScale(interactionSource, pressedScale = 0.94f),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interactionSource,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = stringResource(AppR.string.app_queue_sleep_timer_off),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun <K> QueueToggleChip(
    @DrawableRes iconRes: Int,
    labelKey: K,
    label: String,
    stateDescription: String,
    active: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> inactiveColor.copy(alpha = 0.38f)
            active -> accent
            else -> inactiveColor
        },
        label = "QueueToggleContent",
    )
    val pillColor by animateColorAsState(
        targetValue = if (active && enabled) accent.copy(alpha = 0.18f) else Color.Transparent,
        label = "QueueTogglePill",
    )

    Column(
        modifier = Modifier
            .width(QueueToggleWidth)
            .pressScale(interactionSource, pressedScale = 0.92f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.stateDescription = stateDescription }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .background(color = pillColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedContent(
            targetState = labelKey,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically { it / 2 }) togetherWith
                    (fadeOut(tween(120)) + slideOutVertically { -it / 2 })
            },
            label = "QueueToggleLabel",
        ) { _ ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

/** Marcador dentro de la lista: la canción tras la que el temporizador pausará la música. */
@Composable
internal fun SleepTimerMarker(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(accent.copy(alpha = 0.4f)),
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_timer),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(AppR.string.app_queue_sleep_timer_marker),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(accent.copy(alpha = 0.4f)),
        )
    }
}

/** Pie de la lista cuando la fila se repetirá entera. */
@Composable
internal fun RepeatAllFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_repeat_round),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(AppR.string.app_queue_repeat_all_footer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ecualizador de 3 barras para la canción actual; quieto en pausa. */
@Composable
internal fun NowPlayingBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "NowPlayingBars")
        val heights = BAR_DURATIONS_MILLIS.mapIndexed { index, duration ->
            transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 140),
                ),
                label = "NowPlayingBar$index",
            )
        }
        EqualizerBars(fractions = { index -> heights[index].value }, color = color, modifier = modifier)
    } else {
        EqualizerBars(fractions = { index -> PAUSED_BAR_FRACTIONS[index] }, color = color, modifier = modifier)
    }
}

@Composable
private fun EqualizerBars(
    fractions: (Int) -> Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(14.dp)) {
        val barWidth = size.width / 5f
        repeat(BAR_COUNT) { index ->
            val barHeight = (fractions(index) * size.height).coerceAtLeast(barWidth)
            drawRoundRect(
                color = color,
                topLeft = Offset(x = index * barWidth * 2f, y = size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

/**
 * Índice de la pista durante la cual el temporizador pausará la música, o null si termina
 * después de la fila. Con "repetir canción" siempre es la actual; con "repetir todo" da la vuelta.
 */
internal fun sleepTimerEndIndex(
    queue: List<Track>,
    currentIndex: Int,
    positionMillis: Long,
    remainingMillis: Long,
    repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.Off,
): Int? {
    if (currentIndex !in queue.indices || remainingMillis <= 0L) return null
    if (repeatMode == PlaybackRepeatMode.One) return currentIndex

    var remaining = remainingMillis - (queue[currentIndex].durationMillis - positionMillis).coerceAtLeast(0L)
    if (remaining <= 0L) return currentIndex

    val totalQueueMillis = queue.sumOf { it.durationMillis.coerceAtLeast(0L) }
    if (repeatMode == PlaybackRepeatMode.All && totalQueueMillis > 0L) {
        // Saltar vueltas completas para no iterar de más con temporizadores largos.
        remaining %= totalQueueMillis
        if (remaining == 0L) return currentIndex
    }

    val steps = if (repeatMode == PlaybackRepeatMode.All) queue.size else queue.size - currentIndex - 1
    for (step in 1..steps) {
        val index = (currentIndex + step) % queue.size
        remaining -= queue[index].durationMillis.coerceAtLeast(0L)
        if (remaining <= 0L) return index
    }
    return null
}

private val QueueToggleWidth = 96.dp
private val SLEEP_TIMER_PRESETS_MINUTES = listOf(15, 30, 45, 60)
private const val MILLIS_PER_MINUTE = 60_000L
private const val BAR_COUNT = 3
private val BAR_DURATIONS_MILLIS = listOf(420, 520, 380)
private val PAUSED_BAR_FRACTIONS = listOf(0.45f, 0.8f, 0.6f)
