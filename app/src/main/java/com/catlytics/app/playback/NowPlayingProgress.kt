package com.catlytics.app.playback

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.format.TrackDurationFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingProgress(
    positionMillis: Long,
    bufferedPositionMillis: Long,
    durationMillis: Long,
    enabled: Boolean,
    accent: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var pendingProgress by remember {
        mutableFloatStateOf(positionMillis.progressFor(durationMillis))
    }
    var isSeeking by remember { mutableStateOf(false) }
    var showRemainingTime by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = isDragged || isPressed
    val trackEnabled = enabled && durationMillis > 0L

    val thumbSize by animateDpAsState(
        targetValue = if (isActive) THUMB_ACTIVE_SIZE else THUMB_IDLE_SIZE,
        label = "NowPlayingThumbSize",
    )
    val trackHeight by animateDpAsState(
        targetValue = if (isActive) TRACK_ACTIVE_HEIGHT else TRACK_IDLE_HEIGHT,
        label = "NowPlayingTrackHeight",
    )
    val positionColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "NowPlayingPositionColor",
    )

    val displayedPositionMillis = displayedPositionMillis(
        isSeeking = isSeeking,
        pendingProgress = pendingProgress,
        positionMillis = positionMillis,
        durationMillis = durationMillis,
    )
    val positionText = TrackDurationFormat.format(displayedPositionMillis)
    val endText = if (showRemainingTime) {
        stringResource(
            AppR.string.app_now_playing_remaining_time,
            TrackDurationFormat.format(remainingTimeMillis(displayedPositionMillis, durationMillis)),
        )
    } else {
        TrackDurationFormat.format(durationMillis)
    }
    val bufferedProgress = bufferedPositionMillis.progressFor(durationMillis)
    val timeStyle = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")

    LaunchedEffect(positionMillis, durationMillis) {
        if (!isSeeking) {
            pendingProgress = positionMillis.progressFor(durationMillis)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = pendingProgress,
            onValueChange = {
                isSeeking = true
                pendingProgress = it
            },
            onValueChangeFinished = {
                onSeekTo((durationMillis * pendingProgress).toLong())
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                isSeeking = false
            },
            enabled = trackEnabled,
            valueRange = 0f..1f,
            interactionSource = interactionSource,
            thumb = {
                Box(
                    modifier = Modifier.size(THUMB_TOUCH_SIZE),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(thumbSize)
                            .background(
                                color = if (trackEnabled) {
                                    accent
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            },
            track = { sliderState ->
                val activeColor = if (trackEnabled) {
                    accent
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                }
                val bufferedColor = if (trackEnabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                }
                val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(CircleShape)
                        .background(inactiveColor),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(
                                bufferedProgress
                                    .coerceAtLeast(sliderState.value)
                                    .coerceIn(0f, 1f),
                            )
                            .background(bufferedColor),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(sliderState.value.coerceIn(0f, 1f))
                            .background(activeColor),
                    )
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = positionText,
                style = timeStyle,
                color = positionColor,
            )
            Text(
                text = endText,
                style = timeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        enabled = trackEnabled,
                        role = Role.Button,
                        onClickLabel = if (showRemainingTime) {
                            stringResource(AppR.string.app_now_playing_show_total_time)
                        } else {
                            stringResource(AppR.string.app_now_playing_show_remaining_time)
                        },
                        onClick = { showRemainingTime = !showRemainingTime },
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

internal fun displayedPositionMillis(
    isSeeking: Boolean,
    pendingProgress: Float,
    positionMillis: Long,
    durationMillis: Long,
): Long = if (isSeeking) {
    (durationMillis * pendingProgress).toLong().coerceIn(0L, durationMillis)
} else {
    positionMillis
}

internal fun remainingTimeMillis(positionMillis: Long, durationMillis: Long): Long =
    (durationMillis - positionMillis).coerceAtLeast(0L)

private fun Long.progressFor(durationMillis: Long): Float =
    if (durationMillis > 0L) {
        (toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        0f
    }

private val THUMB_TOUCH_SIZE = 28.dp
private val THUMB_IDLE_SIZE = 10.dp
private val THUMB_ACTIVE_SIZE = 16.dp
private val TRACK_IDLE_HEIGHT = 4.dp
private val TRACK_ACTIVE_HEIGHT = 8.dp
