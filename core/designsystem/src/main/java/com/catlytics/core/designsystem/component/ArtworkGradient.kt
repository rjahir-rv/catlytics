package com.catlytics.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.palette.graphics.Palette
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtworkGradientColors(
    val start: Color,
    val center: Color,
    val end: Color,
)

@Composable
fun ArtworkGradientBackground(
    colors: ArtworkGradientColors,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.start, colors.center, colors.end),
                ),
            ),
        content = content,
    )
}

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

/**
 * Anima un color de cromo (barra superior, barra de estado) hacia [target] con la misma duración
 * que [animateArtworkGradientColors]. Sin destino usa [fallback]; el primer destino se aplica sin
 * animación para que la barra coincida con el degradado desde que aparece la pantalla.
 */
@Composable
fun animateArtworkChromeColor(target: Color?, fallback: Color): Color {
    val color = remember { Animatable(target ?: fallback) }
    var hadTarget by remember { mutableStateOf(target != null) }
    val currentFallback by rememberUpdatedState(fallback)
    LaunchedEffect(target) {
        if (target != null && !hadTarget) {
            color.snapTo(target)
        } else {
            color.animateTo(target ?: currentFallback, tween(ARTWORK_GRADIENT_ANIMATION_MILLIS))
        }
        hadTarget = target != null
    }
    return if (target == null && !color.isRunning) fallback else color.value
}

fun nameSeededGradient(
    seed: String,
    isDark: Boolean,
    surface: Color,
): ArtworkGradientColors {
    val hue = ((seed.trim().lowercase().hashCode() % 360) + 360) % 360
    return if (isDark) {
        ArtworkGradientColors(
            start = Color.hsl(hue.toFloat(), 0.55f, 0.40f),
            center = Color.hsl(((hue + 28) % 360).toFloat(), 0.45f, 0.22f),
            end = surface,
        )
    } else {
        ArtworkGradientColors(
            start = Color.hsl(hue.toFloat(), 0.65f, 0.74f),
            center = Color.hsl(((hue + 28) % 360).toFloat(), 0.55f, 0.87f),
            end = surface,
        )
    }
}

/**
 * Degradado de fondo de una pantalla de detalle a partir de su carátula. Empieza en `noArtwork` y,
 * cuando la carátula carga ([onArtworkLoaded]), pasa a los colores de su paleta. [colors] es el
 * destino sin animar: la pantalla lo anima con [animateArtworkGradientColors].
 */
@Stable
class ArtworkGradientState internal constructor(
    val artworkRequest: ImageRequest,
    initialColors: ArtworkGradientColors,
) {
    internal var bitmap by mutableStateOf<Bitmap?>(null)

    var colors by mutableStateOf(initialColors)
        internal set

    fun onArtworkLoaded(bitmap: Bitmap) {
        this.bitmap = bitmap
    }
}

@Composable
fun rememberArtworkGradient(
    artworkUri: String?,
    fallback: ArtworkGradientColors = rememberFallbackArtworkGradientColors(),
    noArtwork: ArtworkGradientColors = fallback,
    surfaceBlend: Float = ARTWORK_GRADIENT_SURFACE_BLEND,
): ArtworkGradientState {
    val platformContext = LocalPlatformContext.current
    val state = remember(platformContext, artworkUri) {
        ArtworkGradientState(
            artworkRequest = ImageRequest.Builder(platformContext)
                .data(artworkUri)
                .allowHardware(false)
                .build(),
            initialColors = noArtwork,
        )
    }
    val bitmap = state.bitmap
    LaunchedEffect(state, bitmap, fallback, noArtwork, surfaceBlend) {
        state.colors = bitmap?.extractArtworkGradientColors(fallback, surfaceBlend) ?: noArtwork
    }
    return state
}

suspend fun Bitmap.extractArtworkGradientColors(
    fallback: ArtworkGradientColors,
    surfaceBlend: Float = ARTWORK_GRADIENT_SURFACE_BLEND,
): ArtworkGradientColors = withContext(Dispatchers.Default) {
    Palette.from(this@extractArtworkGradientColors)
        .maximumColorCount(PALETTE_MAX_COLOR_COUNT)
        .generate()
        .toArtworkGradientColors(fallback, surfaceBlend.coerceIn(0f, 1f))
}

suspend fun Bitmap.extractArtworkAccentColor(): Color = withContext(Dispatchers.Default) {
    val palette = Palette.from(this@extractArtworkAccentColor)
        .maximumColorCount(PALETTE_MAX_COLOR_COUNT)
        .generate()
    (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let(::Color) ?: Color.Unspecified
}

private fun Palette.toArtworkGradientColors(
    fallback: ArtworkGradientColors,
    surfaceBlend: Float,
): ArtworkGradientColors {
    val dominant = dominantSwatch?.rgb?.let(::Color) ?: fallback.start
    val vibrant = vibrantSwatch?.rgb?.let(::Color) ?: dominant
    val muted = mutedSwatch?.rgb?.let(::Color) ?: dominant

    return ArtworkGradientColors(
        start = vibrant.blendWith(fallback.start, surfaceBlend),
        center = dominant.blendWith(fallback.center, surfaceBlend),
        end = muted.blendWith(fallback.end, surfaceBlend),
    )
}

private fun Color.blendWith(surface: Color, surfaceBlend: Float): Color =
    lerp(this, surface, surfaceBlend)

private const val ARTWORK_GRADIENT_ANIMATION_MILLIS = 500
private const val ARTWORK_GRADIENT_SURFACE_BLEND = 0.58f
private const val PALETTE_MAX_COLOR_COUNT = 16
