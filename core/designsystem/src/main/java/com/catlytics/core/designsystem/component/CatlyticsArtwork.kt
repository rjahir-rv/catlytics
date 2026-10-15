package com.catlytics.core.designsystem.component

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import coil3.toBitmap

@Composable
fun CatlyticsArtwork(
    model: Any?,
    seed: String,
    @DrawableRes placeholderIcon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onSuccess: ((Bitmap) -> Unit)? = null,
) {
    Box(
        modifier = modifier.artworkSemantics(contentDescription),
        contentAlignment = Alignment.Center,
    ) {
        ArtworkPlaceholder(
            seed = seed,
            icon = placeholderIcon,
            modifier = Modifier.fillMaxSize(),
        )
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = contentScale,
                onSuccess = onSuccess?.let { callback ->
                    { state -> callback(state.result.image.toBitmap()) }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Degradado estable por nombre con un icono centrado, para cuando no hay carátula. */
@Composable
fun ArtworkPlaceholder(
    seed: String,
    @DrawableRes icon: Int?,
    modifier: Modifier = Modifier,
    gradient: ArtworkGradientColors = rememberNameSeededGradient(seed),
    iconTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = PLACEHOLDER_ICON_ALPHA),
    iconFraction: Float = PLACEHOLDER_ICON_FRACTION,
) {
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(gradient.start, gradient.center))),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.fillMaxSize(iconFraction),
            )
        }
    }
}

fun Modifier.artworkSemantics(contentDescription: String?): Modifier =
    if (contentDescription == null) {
        this
    } else {
        semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }

@Composable
fun rememberNameSeededGradient(seed: String): ArtworkGradientColors {
    val surface = MaterialTheme.colorScheme.surface
    val isDark = surface.luminance() < 0.5f
    return remember(seed, isDark, surface) {
        nameSeededGradient(seed, isDark, surface)
    }
}

private const val PLACEHOLDER_ICON_ALPHA = 0.55f
private const val PLACEHOLDER_ICON_FRACTION = 0.4f
