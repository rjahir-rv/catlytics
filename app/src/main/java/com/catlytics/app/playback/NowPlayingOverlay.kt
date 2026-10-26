package com.catlytics.app.playback

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.catlytics.core.navigation.ViewModelStoreScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


@Composable
internal fun NowPlayingOverlay(
    isOpen: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val hiddenFraction = remember { Animatable(if (isOpen) 0f else 1f) }
    val isOpenState = rememberUpdatedState(isOpen)
    val currentOnClose by rememberUpdatedState(onClose)
    val coroutineScope = rememberCoroutineScope()
    val isComposed by remember {
        derivedStateOf { isOpenState.value || hiddenFraction.value < 1f }
    }

    LaunchedEffect(isOpen) {
        hiddenFraction.animateTo(
            targetValue = if (isOpen) 0f else 1f,
            animationSpec = tween(
                durationMillis = NOW_PLAYING_TRANSITION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    PredictiveBackHandler(enabled = isOpen) { backEvents ->
        try {
            backEvents.collect { event ->
                hiddenFraction.snapTo(event.progress * PREDICTIVE_BACK_MAX_HIDDEN_FRACTION)
            }
            currentOnClose()
        } catch (cancellation: CancellationException) {
            coroutineScope.launch { hiddenFraction.animateTo(0f) }
            throw cancellation
        }
    }

    ViewModelStoreScope(key = NowPlayingRoute.takeIf { isComposed }) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { translationY = hiddenFraction.value * size.height }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent()
                    }
                },
        ) {
            content()
        }
    }
}

private const val NOW_PLAYING_TRANSITION_MILLIS = 450
private const val PREDICTIVE_BACK_MAX_HIDDEN_FRACTION = 0.3f
