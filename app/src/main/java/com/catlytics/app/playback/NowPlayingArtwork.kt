package com.catlytics.app.playback

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.Track
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun NowPlayingArtwork(
    track: Track?,
    isPlaying: Boolean,
    accent: Color,
    onArtworkLoaded: (Bitmap) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekBackward10Seconds: () -> Unit,
    onSeekForward10Seconds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkShape = RoundedCornerShape(28.dp)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val enabled = track != null
    var widthPx by remember { mutableIntStateOf(0) }
    val dragOffset = remember { Animatable(0f) }
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }

    // Estilo Apple Music: la carátula se encoge ligeramente en pausa.
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying || !enabled) 1f else PAUSED_ARTWORK_SCALE,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "NowPlayingArtworkScale",
    )

    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(SEEK_FEEDBACK_MILLIS)
            seekFeedback = null
        }
    }

    val previousLabel = stringResource(AppR.string.app_action_previous)
    val nextLabel = stringResource(AppR.string.app_action_next)
    val seekBackLabel = stringResource(AppR.string.app_action_seek_back_10)
    val seekForwardLabel = stringResource(AppR.string.app_action_seek_forward_10)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { widthPx = it.width }
            .semantics {
                if (enabled) {
                    customActions = listOf(
                        CustomAccessibilityAction(previousLabel) { onSkipPrevious(); true },
                        CustomAccessibilityAction(nextLabel) { onSkipNext(); true },
                        CustomAccessibilityAction(seekBackLabel) { onSeekBackward10Seconds(); true },
                        CustomAccessibilityAction(seekForwardLabel) { onSeekForward10Seconds(); true },
                    )
                }
            }
            .draggable(
                orientation = Orientation.Horizontal,
                enabled = enabled,
                state = rememberDraggableState { delta ->
                    scope.launch { dragOffset.snapTo(dragOffset.value + delta) }
                },
                onDragStopped = { velocity ->
                    when (swipeSkipDirection(dragOffset.value, widthPx.toFloat(), velocity)) {
                        ArtworkSwipe.Previous -> {
                            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            onSkipPrevious()
                        }
                        ArtworkSwipe.Next -> {
                            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            onSkipNext()
                        }
                        ArtworkSwipe.None -> Unit
                    }
                    dragOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    )
                },
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = { position ->
                        val forward = position.x >= size.width / 2f
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        if (forward) onSeekForward10Seconds() else onSeekBackward10Seconds()
                        seekFeedback = SeekFeedback(
                            forward = forward,
                            sequence = (seekFeedback?.sequence ?: 0) + 1,
                        )
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = artworkScale * HALO_SCALE
                    scaleY = artworkScale * HALO_SCALE
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.35f),
                            accent.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        PlaybackArtwork(
            artworkUri = track?.artworkUri,
            contentDescription = track?.let {
                stringResource(AppR.string.app_content_description_artwork, it.title)
            },
            onSuccess = onArtworkLoaded,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val dragFraction = if (widthPx > 0) dragOffset.value / widthPx else 0f
                    translationX = dragOffset.value
                    rotationZ = dragFraction * MAX_DRAG_ROTATION_DEGREES
                    alpha = 1f - abs(dragFraction) * 0.4f
                    scaleX = artworkScale
                    scaleY = artworkScale
                }
                .shadow(
                    elevation = if (isPlaying) 20.dp else 8.dp,
                    shape = artworkShape,
                    clip = false,
                    ambientColor = accent,
                    spotColor = accent,
                )
                .clip(artworkShape),
        )
        SeekFeedbackBubble(
            visible = seekFeedback?.forward == false,
            iconRes = R.drawable.ic_replay_10,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp),
        )
        SeekFeedbackBubble(
            visible = seekFeedback?.forward == true,
            iconRes = R.drawable.ic_forward_10,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp),
        )
    }
}

@Composable
private fun SeekFeedbackBubble(
    visible: Boolean,
    iconRes: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut() + scaleOut(targetScale = 1.1f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color = Color.Black.copy(alpha = 0.45f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

private data class SeekFeedback(val forward: Boolean, val sequence: Int)

internal enum class ArtworkSwipe { Previous, Next, None }

/**
 * Decide si un arrastre horizontal sobre la carátula cambia de pista: basta con superar
 * [SWIPE_DISTANCE_FRACTION] del ancho, o un gesto rápido en la misma dirección.
 * Arrastrar hacia la derecha (offset positivo) va a la anterior; hacia la izquierda, a la siguiente.
 */
internal fun swipeSkipDirection(
    offsetPx: Float,
    widthPx: Float,
    velocityPx: Float,
): ArtworkSwipe {
    if (widthPx <= 0f || offsetPx == 0f) return ArtworkSwipe.None
    val fraction = offsetPx / widthPx
    val isFling = abs(velocityPx) >= SWIPE_FLING_VELOCITY &&
        abs(fraction) >= SWIPE_FLING_MIN_FRACTION &&
        (velocityPx > 0f) == (offsetPx > 0f)
    if (abs(fraction) < SWIPE_DISTANCE_FRACTION && !isFling) return ArtworkSwipe.None
    return if (offsetPx > 0f) ArtworkSwipe.Previous else ArtworkSwipe.Next
}

private const val PAUSED_ARTWORK_SCALE = 0.88f
private const val HALO_SCALE = 1.08f
private const val MAX_DRAG_ROTATION_DEGREES = 6f
private const val SEEK_FEEDBACK_MILLIS = 650L
private const val SWIPE_DISTANCE_FRACTION = 0.3f
private const val SWIPE_FLING_MIN_FRACTION = 0.08f
private const val SWIPE_FLING_VELOCITY = 1_500f
