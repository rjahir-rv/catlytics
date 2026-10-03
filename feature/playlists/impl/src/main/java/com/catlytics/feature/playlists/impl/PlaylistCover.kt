package com.catlytics.feature.playlists.impl

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.toBitmap
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientColors
import com.catlytics.core.designsystem.component.nameSeededGradient
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.model.LIKED_PLAYLIST_ID

@Composable
internal fun PlaylistCover(
    playlistId: String,
    name: String,
    artworkModel: Any?,
    mosaicArtworkUris: List<String>,
    modifier: Modifier = Modifier,
    shape: Shape = CatlyticsCorners.Large,
    contentDescription: String? = null,
    onArtworkLoaded: ((Bitmap) -> Unit)? = null,
) {
    var failedUris by remember(mosaicArtworkUris) { mutableStateOf(emptySet<String>()) }
    val availableUris = remember(mosaicArtworkUris, failedUris) {
        mosaicArtworkUris.filterNot { it in failedUris }
    }
    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        PlaylistCoverPlaceholder(
            isLiked = playlistId == LIKED_PLAYLIST_ID,
            seed = name,
        )
        when {
            artworkModel != null -> CoverImage(
                model = artworkModel,
                contentDescription = contentDescription,
                onLoaded = onArtworkLoaded,
                modifier = Modifier.fillMaxSize(),
            )

            playlistId == LIKED_PLAYLIST_ID || availableUris.isEmpty() -> Unit
            availableUris.size == 1 -> CoverImage(
                model = availableUris.first(),
                contentDescription = contentDescription,
                onError = { failedUris = failedUris + availableUris.first() },
                modifier = Modifier.fillMaxSize(),
            )

            else -> PlaylistCoverMosaic(
                artworkUris = availableUris,
                contentDescription = contentDescription,
                onTileError = { uri -> failedUris = failedUris + uri },
            )
        }
    }
}

@Composable
private fun CoverImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onLoaded: ((Bitmap) -> Unit)? = null,
    onError: (() -> Unit)? = null,
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        onError = onError?.let { callback -> { _ -> callback() } },
        onSuccess = onLoaded?.let { callback ->
            { state -> callback(state.result.image.toBitmap()) }
        },
    )
}

/** 2 imágenes: mitades; 3: una alta y dos apiladas; 4 o más: cuadrícula 2×2. */
@Composable
private fun PlaylistCoverMosaic(
    artworkUris: List<String>,
    contentDescription: String?,
    onTileError: (String) -> Unit,
) {
    val gap = 1.dp
    when (artworkUris.size) {
        2 -> Row(Modifier.fillMaxSize()) {
            CoverImage(
                model = artworkUris[0],
                onError = { onTileError(artworkUris[0]) },
                contentDescription = contentDescription,
                modifier = Modifier.weight(1f).fillMaxSize().padding(end = gap),
            )
            CoverImage(
                model = artworkUris[1],
                onError = { onTileError(artworkUris[1]) },
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxSize().padding(start = gap),
            )
        }

        3 -> Row(Modifier.fillMaxSize()) {
            CoverImage(
                model = artworkUris[0],
                onError = { onTileError(artworkUris[0]) },
                contentDescription = contentDescription,
                modifier = Modifier.weight(1f).fillMaxSize().padding(end = gap),
            )
            Column(Modifier.weight(1f).fillMaxSize().padding(start = gap)) {
                CoverImage(
                    model = artworkUris[1],
                onError = { onTileError(artworkUris[1]) },
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = gap),
                )
                CoverImage(
                    model = artworkUris[2],
                    onError = { onTileError(artworkUris[2]) },
                    contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = gap),
                )
            }
        }

        else -> Column(Modifier.fillMaxSize()) {
            repeat(2) { rowIndex ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(2) { columnIndex ->
                        val uri = artworkUris[rowIndex * 2 + columnIndex]
                        CoverImage(
                            model = uri,
                            onError = { onTileError(uri) },
                            contentDescription = if (rowIndex == 0 && columnIndex == 0) {
                                contentDescription
                            } else {
                                null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(
                                    start = if (columnIndex == 1) gap else 0.dp,
                                    end = if (columnIndex == 0) gap else 0.dp,
                                    top = if (rowIndex == 1) gap else 0.dp,
                                    bottom = if (rowIndex == 0) gap else 0.dp,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCoverPlaceholder(
    isLiked: Boolean,
    seed: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val surface = colorScheme.surface
    val gradient = remember(isLiked, seed, isDark, surface) {
        playlistFallbackGradient(isLiked, seed, isDark, surface)
    }
    val iconTint = if (isLiked) {
        if (isDark) Color.White.copy(alpha = 0.92f) else Color.hsl(340f, 0.6f, 0.38f)
    } else {
        colorScheme.onSurface.copy(alpha = 0.55f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(gradient.start, gradient.center))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                if (isLiked) R.drawable.ic_favorite_fill else R.drawable.ic_playlist,
            ),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.fillMaxSize(PLACEHOLDER_ICON_FRACTION),
        )
    }
}

internal fun playlistFallbackGradient(
    isLiked: Boolean,
    seed: String,
    isDark: Boolean,
    surface: Color,
): ArtworkGradientColors = when {
    isLiked && isDark -> ArtworkGradientColors(
        start = Color.hsl(340f, 0.60f, 0.42f),
        center = Color.hsl(320f, 0.50f, 0.26f),
        end = surface,
    )

    isLiked -> ArtworkGradientColors(
        start = Color.hsl(340f, 0.75f, 0.78f),
        center = Color.hsl(325f, 0.65f, 0.90f),
        end = surface,
    )

    else -> nameSeededGradient(seed, isDark, surface)
}

private const val PLACEHOLDER_ICON_FRACTION = 0.4f
