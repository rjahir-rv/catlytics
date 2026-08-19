package com.catlytics.app.playback

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.Track

@Composable
fun PlaybackArtwork(
    artworkUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    allowHardware: Boolean = true,
    onSuccess: ((Bitmap) -> Unit)? = null,
) {
    val context = LocalPlatformContext.current
    val fallback = painterResource(id = R.drawable.placeholder_track)
    var lastSuccessPainter by remember { mutableStateOf<Painter?>(null) }

    LaunchedEffect(artworkUri) {
        if (artworkUri == null) {
            lastSuccessPainter = null
        }
    }

    val request = remember(context, artworkUri, allowHardware) {
        ImageRequest.Builder(context)
            .data(artworkUri)
            .allowHardware(allowHardware)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        placeholder = lastSuccessPainter ?: fallback.takeIf { artworkUri != null },
        error = fallback,
        fallback = fallback,
        onSuccess = { state ->
            val bitmap = state.result.image.toBitmap()
            lastSuccessPainter = BitmapPainter(bitmap.asImageBitmap())
            onSuccess?.invoke(bitmap.asSoftwareBitmap())
        },
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
fun PrefetchAdjacentPlaybackArtwork(
    queue: List<Track>,
    currentIndex: Int,
) {
    val context = LocalPlatformContext.current
    val uris = remember(queue, currentIndex) {
        adjacentArtworkUris(queue, currentIndex)
    }

    LaunchedEffect(context, uris) {
        if (uris.isEmpty()) return@LaunchedEffect
        val imageLoader = context.imageLoader
        uris.forEach { uri ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(uri)
                    .build(),
            )
        }
    }
}

internal fun adjacentArtworkUris(
    queue: List<Track>,
    currentIndex: Int,
    radius: Int = ADJACENT_ARTWORK_PREFETCH_RADIUS,
): List<String> {
    if (queue.isEmpty() || radius <= 0) return emptyList()

    val uris = LinkedHashSet<String>()
    for (offset in 1..radius) {
        queue.getOrNull(currentIndex - offset)?.artworkUri?.let(uris::add)
        queue.getOrNull(currentIndex + offset)?.artworkUri?.let(uris::add)
    }
    return uris.toList()
}

internal fun Bitmap.asSoftwareBitmap(): Bitmap =
    if (config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false) ?: this
    } else {
        this
    }

private const val ADJACENT_ARTWORK_PREFETCH_RADIUS = 2
