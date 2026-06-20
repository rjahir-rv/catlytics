package com.catlytics.feature.library.impl.album

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientBackground
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.extractArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberFallbackArtworkGradientColors
import com.catlytics.core.model.Album
import com.catlytics.core.model.Track
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun LibraryAlbumScreen(
    uiState: LibraryAlbumUiState,
    modifier: Modifier = Modifier,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onTopBarColorChange: (Color) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    when (uiState) {
        LibraryAlbumUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        LibraryAlbumUiState.NotFound -> AlbumMessage(
            message = "Este álbum ya no está disponible.",
            modifier = modifier,
        )
        is LibraryAlbumUiState.Error -> AlbumMessage(uiState.message, modifier)
        is LibraryAlbumUiState.Success -> {
            val content = uiState.content
            val platformContext = LocalPlatformContext.current
            val fallbackGradient = rememberFallbackArtworkGradientColors()
            val artworkRequest = remember(platformContext, content.album.artworkUri) {
                ImageRequest.Builder(platformContext)
                    .data(content.album.artworkUri)
                    .allowHardware(false)
                    .build()
            }
            var artworkBitmap by remember(content.album.artworkUri) { mutableStateOf<Bitmap?>(null) }
            var gradientColors by remember { mutableStateOf(fallbackGradient) }
            val animatedGradientColors = animateArtworkGradientColors(
                target = gradientColors,
                labelPrefix = "LibraryAlbumGradient",
            )

            LaunchedEffect(animatedGradientColors.start) {
                onTopBarColorChange(animatedGradientColors.start)
            }

            LaunchedEffect(content.album.artworkUri, artworkBitmap, fallbackGradient) {
                gradientColors = artworkBitmap?.extractArtworkGradientColors(fallbackGradient) ?: fallbackGradient
            }

            ArtworkGradientBackground(
                colors = animatedGradientColors,
                modifier = modifier,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 20.dp,
                        end = 20.dp,
                        bottom = bottomPadding() + 20.dp,
                    ),
                ) {
                    item(key = "header") {
                        AlbumHeader(
                            album = content.album,
                            artworkModel = artworkRequest,
                            onArtworkLoaded = { artworkBitmap = it },
                        )
                    }
                    itemsIndexed(
                        items = content.tracks,
                        key = { _, track -> track.id },
                    ) { index, track ->
                        AlbumTrackRow(
                            position = index + 1,
                            track = track,
                            onClick = { onTrackSelected(track, content.tracks) },
                            onTrackOptions = { onTrackOptions(track) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    album: Album,
    artworkModel: Any?,
    onArtworkLoaded: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = artworkModel,
            contentDescription = "Portada de ${album.title}",
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
            placeholder = painterResource(R.drawable.placeholder_album),
            error = painterResource(R.drawable.placeholder_album),
            fallback = painterResource(R.drawable.placeholder_album),
            onSuccess = { state ->
                onArtworkLoaded(state.result.image.toBitmap())
            },
            contentScale = ContentScale.Crop,
        )
        Text(
            text = album.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artist.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = album.trackCount.trackCountLabel(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlbumTrackRow(
    position: Int,
    track: Track,
    onClick: () -> Unit,
    onTrackOptions: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = track.durationMillis.formatDuration(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onTrackOptions) {
            Icon(painterResource(R.drawable.ic_options), "Opciones de ${track.title}")
        }
    }
}

@Composable
private fun AlbumMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Int.trackCountLabel() = if (this == 1) "1 canción" else "$this canciones"

private fun Long.formatDuration(): String {
    val totalSeconds = milliseconds.inWholeSeconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
