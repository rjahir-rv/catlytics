package com.catlytics.app.playback

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track

@Composable
internal fun NowPlayingTransportControls(
    playbackState: PlaybackState,
    enabled: Boolean,
    accent: Color,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val repeatStateDescription = when (playbackState.repeatMode) {
        PlaybackRepeatMode.Off -> stringResource(AppR.string.app_repeat_state_off)
        PlaybackRepeatMode.One -> stringResource(AppR.string.app_repeat_state_one)
        PlaybackRepeatMode.All -> stringResource(AppR.string.app_repeat_state_all)
    }
    val repeatActionDescription = when (playbackState.repeatMode) {
        PlaybackRepeatMode.Off -> stringResource(AppR.string.app_action_enable_repeat_one)
        PlaybackRepeatMode.One -> stringResource(AppR.string.app_action_enable_repeat_all)
        PlaybackRepeatMode.All -> stringResource(AppR.string.app_action_disable_repeat)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeToggleButton(
            iconRes = R.drawable.ic_shuffle,
            contentDescription = if (playbackState.isShuffleEnabled) {
                stringResource(AppR.string.app_action_disable_shuffle)
            } else {
                stringResource(AppR.string.app_action_enable_shuffle)
            },
            active = playbackState.isShuffleEnabled,
            enabled = enabled,
            accent = accent,
            onClick = {
                haptics.performHapticFeedback(
                    if (playbackState.isShuffleEnabled) {
                        HapticFeedbackType.ToggleOff
                    } else {
                        HapticFeedbackType.ToggleOn
                    },
                )
                onToggleShuffle()
            },
        )
        TransportButton(
            iconRes = R.drawable.ic_skip_back_fill,
            contentDescription = stringResource(AppR.string.app_action_previous),
            enabled = enabled,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onSkipPrevious()
            },
        )
        PlayPauseButton(
            status = playbackState.status,
            enabled = enabled,
            accent = accent,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onTogglePlayback()
            },
        )
        TransportButton(
            iconRes = R.drawable.ic_skip_next_fill,
            contentDescription = stringResource(AppR.string.app_action_next),
            enabled = enabled,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onSkipNext()
            },
        )
        ModeToggleButton(
            iconRes = if (playbackState.repeatMode == PlaybackRepeatMode.One) {
                R.drawable.ic_repeat_one
            } else {
                R.drawable.ic_repeat_round
            },
            contentDescription = repeatActionDescription,
            active = playbackState.repeatMode != PlaybackRepeatMode.Off,
            enabled = enabled,
            accent = accent,
            stateDescription = repeatStateDescription,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onCycleRepeatMode()
            },
        )
    }
}

@Composable
internal fun NowPlayingActionBar(
    track: Track?,
    upNext: Track?,
    isSleepTimerActive: Boolean,
    hasQueue: Boolean,
    onSeekBackward10Seconds: () -> Unit,
    onSeekForward10Seconds: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = track != null
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryIconButton(
            iconRes = R.drawable.ic_replay_10,
            contentDescription = stringResource(AppR.string.app_action_seek_back_10),
            enabled = enabled,
            onClick = onSeekBackward10Seconds,
        )
        if (enabled) {
            UpNextChip(
                upNext = upNext,
                isSleepTimerActive = isSleepTimerActive,
                enabled = hasQueue,
                onClick = onOpenQueue,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        SecondaryIconButton(
            iconRes = R.drawable.ic_forward_10,
            contentDescription = stringResource(AppR.string.app_action_seek_forward_10),
            enabled = enabled,
            onClick = onSeekForward10Seconds,
        )
    }
}

@Composable
private fun UpNextChip(
    upNext: Track?,
    isSleepTimerActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = upNext?.let {
        stringResource(AppR.string.app_queue_up_next, it.title)
    } ?: stringResource(AppR.string.app_queue_title)

    Surface(
        modifier = modifier
            .pressScale(interactionSource, pressedScale = 0.96f)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = stringResource(AppR.string.app_action_open_queue),
                onClick = onClick,
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        contentColor = MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (enabled) 0.85f else 0.38f,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(
                    id = if (isSleepTimerActive) R.drawable.ic_timer else R.drawable.ic_queue_music,
                ),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayPauseButton(
    status: PlaybackStatus,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isBuffering = status == PlaybackStatus.Buffering
    val showPauseIcon = status == PlaybackStatus.Playing || isBuffering
    // Círculo en pausa/buffering; squircle mientras reproduce (forma expresiva de M3).
    val cornerRadius by animateDpAsState(
        targetValue = if (status == PlaybackStatus.Playing) {
            PLAY_BUTTON_PLAYING_CORNER
        } else {
            PLAY_BUTTON_SIZE / 2
        },
        label = "PlayButtonCorner",
    )
    val containerColor by animateColorAsState(
        targetValue = if (enabled) {
            accent
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        },
        label = "PlayButtonContainer",
    )
    val contentColor = if (enabled) {
        accent.readableContentColor()
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Box(
        modifier = Modifier.size(PLAY_BUTTON_SIZE + 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(PLAY_BUTTON_SIZE + 10.dp),
                color = accent,
                strokeWidth = 2.dp,
                trackColor = Color.Transparent,
            )
        }
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(PLAY_BUTTON_SIZE)
                .pressScale(interactionSource, pressedScale = 0.92f),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Crossfade(
                    targetState = showPauseIcon,
                    animationSpec = tween(durationMillis = PLAYBACK_ICON_CROSSFADE_MILLIS),
                    label = "PlaybackIconCrossfade",
                ) { pause ->
                    Icon(
                        painter = painterResource(
                            id = if (pause) R.drawable.ic_pause_fill else R.drawable.ic_play_fill,
                        ),
                        contentDescription = if (pause) {
                            stringResource(AppR.string.app_action_pause)
                        } else {
                            stringResource(AppR.string.app_action_play)
                        },
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(56.dp)
            .pressScale(interactionSource, pressedScale = 0.88f),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun ModeToggleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    active: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    stateDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint by animateColorAsState(
        targetValue = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "ModeToggleTint",
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (active && enabled) 1f else 0f,
        label = "ModeToggleIndicator",
    )

    Box(contentAlignment = Alignment.Center) {
        IconToggleButton(
            checked = active,
            onCheckedChange = { onClick() },
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(48.dp)
                .pressScale(interactionSource, pressedScale = 0.88f)
                .then(
                    if (stateDescription != null) {
                        Modifier.semantics { this.stateDescription = stateDescription }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 2.dp)
                .size(4.dp)
                .graphicsLayer { alpha = indicatorAlpha }
                .background(color = accent, shape = CircleShape),
        )
    }
}

@Composable
private fun SecondaryIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(48.dp)
            .pressScale(interactionSource, pressedScale = 0.88f),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Texto/ícono legible sobre un fondo [this] (acento de la carátula). */
internal fun Color.readableContentColor(): Color =
    if (luminance() > 0.5f) Color.Black else Color.White

/**
 * Siguiente pista de la fila. La fila ya refleja el orden de reproducción (incluso con mezcla),
 * así que basta con el índice siguiente; con "repetir todo" vuelve al inicio.
 */
internal fun upNextTrack(
    queue: List<Track>,
    currentIndex: Int,
    repeatMode: PlaybackRepeatMode,
): Track? {
    if (queue.size < 2 || currentIndex !in queue.indices) return null
    return queue.getOrNull(currentIndex + 1)
        ?: queue.first().takeIf { repeatMode == PlaybackRepeatMode.All }
}

private val PLAY_BUTTON_SIZE = 76.dp
private val PLAY_BUTTON_PLAYING_CORNER = 24.dp
private const val PLAYBACK_ICON_CROSSFADE_MILLIS = 150
