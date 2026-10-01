package com.catlytics.core.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

fun navigationForwardTransition() =
    (slideInHorizontally(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        initialOffsetX = { fullWidth -> fullWidth / 8 },
    ) + fadeIn(
        animationSpec = tween(durationMillis = NAVIGATION_TRANSITION_MILLIS),
    )) togetherWith (slideOutHorizontally(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        targetOffsetX = { fullWidth -> -fullWidth / 16 },
    ) + fadeOut(
        animationSpec = tween(durationMillis = NAVIGATION_TRANSITION_MILLIS),
    ))

fun navigationBackTransition() =
    (slideInHorizontally(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        initialOffsetX = { fullWidth -> -fullWidth / 16 },
    ) + fadeIn(
        animationSpec = tween(durationMillis = NAVIGATION_TRANSITION_MILLIS),
    )) togetherWith (slideOutHorizontally(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        targetOffsetX = { fullWidth -> fullWidth / 8 },
    ) + fadeOut(
        animationSpec = tween(durationMillis = NAVIGATION_TRANSITION_MILLIS),
    ))

fun verticalSheetEnterTransition() =
    slideInVertically(
        animationSpec = tween(
            durationMillis = VERTICAL_SHEET_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        initialOffsetY = { fullHeight -> fullHeight },
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = VERTICAL_SHEET_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
    ) togetherWith fadeOut(
        animationSpec = tween(
            durationMillis = VERTICAL_SHEET_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
    )

fun verticalSheetExitTransition() =
    (fadeIn(
        animationSpec = tween(
            durationMillis = VERTICAL_SHEET_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
    ) togetherWith slideOutVertically(
        animationSpec = tween(
            durationMillis = VERTICAL_SHEET_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        targetOffsetY = { fullHeight -> fullHeight },
    )).apply {
        targetContentZIndex = -1f
    }

private const val VERTICAL_SHEET_TRANSITION_MILLIS = 450
private const val NAVIGATION_TRANSITION_MILLIS = 280
