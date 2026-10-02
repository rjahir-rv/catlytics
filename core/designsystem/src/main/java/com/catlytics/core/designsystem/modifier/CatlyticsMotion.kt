package com.catlytics.core.designsystem.modifier

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.98f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.staggeredEntrance(index: Int, animate: Boolean = true): Modifier {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay((index * ENTRANCE_STAGGER_MILLIS).milliseconds)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(ENTRANCE_DURATION_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * ENTRANCE_OFFSET.toPx()
    }
}

private const val ENTRANCE_STAGGER_MILLIS = 50L
private const val ENTRANCE_DURATION_MILLIS = 380
private val ENTRANCE_OFFSET = 16.dp
