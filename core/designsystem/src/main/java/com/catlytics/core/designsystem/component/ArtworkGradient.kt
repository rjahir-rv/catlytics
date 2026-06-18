package com.catlytics.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtworkGradientColors(
    val start: Color,
    val center: Color,
    val end: Color,
)

@Composable
fun rememberFallbackArtworkGradientColors(): ArtworkGradientColors {
    val colorScheme = MaterialTheme.colorScheme
    return remember(
        colorScheme.surface,
        colorScheme.surfaceContainer,
        colorScheme.surfaceContainerHighest,
    ) {
        ArtworkGradientColors(
            start = colorScheme.surfaceContainerHighest,
            center = colorScheme.surfaceContainer,
            end = colorScheme.surface,
        )
    }
}

@Composable
fun animateArtworkGradientColors(
    target: ArtworkGradientColors,
    labelPrefix: String,
): ArtworkGradientColors {
    val start by animateColorAsState(
        targetValue = target.start,
        animationSpec = tween(ARTWORK_GRADIENT_ANIMATION_MILLIS),
        label = "${labelPrefix}Start",
    )
    val center by animateColorAsState(
        targetValue = target.center,
        animationSpec = tween(ARTWORK_GRADIENT_ANIMATION_MILLIS),
        label = "${labelPrefix}Center",
    )
    val end by animateColorAsState(
        targetValue = target.end,
        animationSpec = tween(ARTWORK_GRADIENT_ANIMATION_MILLIS),
        label = "${labelPrefix}End",
    )

    return ArtworkGradientColors(start = start, center = center, end = end)
}

suspend fun Bitmap.extractArtworkGradientColors(
    fallback: ArtworkGradientColors,
): ArtworkGradientColors = withContext(Dispatchers.Default) {
    Palette.from(this@extractArtworkGradientColors)
        .maximumColorCount(PALETTE_MAX_COLOR_COUNT)
        .generate()
        .toArtworkGradientColors(fallback)
}

private fun Palette.toArtworkGradientColors(
    fallback: ArtworkGradientColors,
): ArtworkGradientColors {
    val dominant = dominantSwatch?.rgb?.let(::Color) ?: fallback.start
    val vibrant = vibrantSwatch?.rgb?.let(::Color) ?: dominant
    val muted = mutedSwatch?.rgb?.let(::Color) ?: dominant

    return ArtworkGradientColors(
        start = vibrant.blendWith(fallback.start),
        center = dominant.blendWith(fallback.center),
        end = muted.blendWith(fallback.end),
    )
}

private fun Color.blendWith(surface: Color): Color =
    lerp(this, surface, ARTWORK_GRADIENT_SURFACE_BLEND)

private const val ARTWORK_GRADIENT_ANIMATION_MILLIS = 500
private const val ARTWORK_GRADIENT_SURFACE_BLEND = 0.58f
private const val PALETTE_MAX_COLOR_COUNT = 16
