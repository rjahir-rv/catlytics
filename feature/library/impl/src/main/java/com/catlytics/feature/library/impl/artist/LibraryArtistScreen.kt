package com.catlytics.feature.library.impl.artist

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun LibraryArtistScreen(
    uiState: LibraryArtistUiState,
    onAlbumSelected: (Album) -> Unit,
    modifier: Modifier = Modifier,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onTopBarColorChange: (Color) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    when (uiState) {
        LibraryArtistUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        LibraryArtistUiState.NotFound -> ArtistMessage(
            message = "Este artista ya no está disponible.",
            modifier = modifier,
        )
        is LibraryArtistUiState.Error -> ArtistMessage(uiState.message, modifier)
        is LibraryArtistUiState.Success -> ArtistContent(
            content = uiState.content,
            onAlbumSelected = onAlbumSelected,
            onTrackSelected = onTrackSelected,
            onAddToPlaylist = onAddToPlaylist,
            onTrackOptions = onTrackOptions,
            onTopBarColorChange = onTopBarColorChange,
            bottomPadding = bottomPadding,
            modifier = modifier,
        )
    }
}

@Composable
private fun ArtistContent(
    content: ArtistContent,
    onAlbumSelected: (Album) -> Unit,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onTopBarColorChange: (Color) -> Unit,
    bottomPadding: () -> Dp,
    modifier: Modifier = Modifier,
) {
    val platformContext = LocalPlatformContext.current
    val fallbackGradient = rememberFallbackArtworkGradientColors()
    val artworkRequest = remember(platformContext, content.summary.artworkUri) {
        ImageRequest.Builder(platformContext)
            .data(content.summary.artworkUri)
            .allowHardware(false)
            .build()
    }
    var artworkBitmap by remember(content.summary.artworkUri) { mutableStateOf<Bitmap?>(null) }
    var gradientColors by remember { mutableStateOf(fallbackGradient) }
    val animatedGradientColors = animateArtworkGradientColors(
        target = gradientColors,
        labelPrefix = "LibraryArtistGradient",
    )

    LaunchedEffect(animatedGradientColors.start) {
        onTopBarColorChange(animatedGradientColors.start)
    }

    LaunchedEffect(content.summary.artworkUri, artworkBitmap, fallbackGradient) {
        gradientColors = artworkBitmap?.extractArtworkGradientColors(fallbackGradient) ?: fallbackGradient
    }

    ArtworkGradientBackground(
        colors = animatedGradientColors,
        modifier = modifier,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = bottomPadding() + 20.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                ArtistHeader(
                    content = content,
                    artworkModel = artworkRequest,
                    onArtworkLoaded = { artworkBitmap = it },
                )
            }
            item(key = "albums-title", span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Álbumes")
            }
            items(items = content.albums, key = Album::id) { album ->
                ArtistAlbumCard(
                    album = album,
                    onClick = { onAlbumSelected(album) },
                    onAddToPlaylist = { onAddToPlaylist(PlaylistSource.AlbumSource(album.id)) },
                )
            }
            item(key = "songs-title", span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(
                    text = "Canciones",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(
                items = content.tracks,
                key = Track::id,
                span = { GridItemSpan(maxLineSpan) },
            ) { track ->
                ArtistTrackRow(
                    track = track,
                    onClick = { onTrackSelected(track, content.tracks) },
                    onTrackOptions = { onTrackOptions(track) },
                )
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    content: ArtistContent,
    artworkModel: Any?,
    onArtworkLoaded: (Bitmap) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = artworkModel,
            contentDescription = "Imagen de ${content.summary.artist.name}",
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .aspectRatio(1f)
                .clip(CircleShape),
            placeholder = painterResource(R.drawable.placeholder_artist),
            error = painterResource(R.drawable.placeholder_artist),
            fallback = painterResource(R.drawable.placeholder_artist),
            onSuccess = { state ->
                onArtworkLoaded(state.result.image.toBitmap())
            },
            contentScale = ContentScale.Crop,
        )
        Text(
            text = content.summary.artist.name,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${content.summary.albumCount} álbumes · ${content.summary.trackCount} canciones",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArtistAlbumCard(
    album: Album,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AsyncImage(
            model = album.artworkUri,
            contentDescription = "Portada de ${album.title}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
            placeholder = painterResource(R.drawable.placeholder_album),
            error = painterResource(R.drawable.placeholder_album),
            fallback = painterResource(R.drawable.placeholder_album),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onAddToPlaylist) {
            Icon(painterResource(R.drawable.ic_options), "Opciones de ${album.title}")
        }
        Text(
            text = if (album.trackCount == 1) "1 canción" else "${album.trackCount} canciones",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArtistTrackRow(
    track: Track,
    onClick: () -> Unit,
    onTrackOptions: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                placeholder = painterResource(R.drawable.placeholder_album),
                error = painterResource(R.drawable.placeholder_album),
                fallback = painterResource(R.drawable.placeholder_album),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = track.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onTrackOptions) {
                Icon(painterResource(R.drawable.ic_options), "Opciones de ${track.title}")
            }
            Text(
                text = track.durationMillis.formatDuration(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun ArtistMessage(
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

private fun Long.formatDuration(): String {
    val totalSeconds = milliseconds.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
