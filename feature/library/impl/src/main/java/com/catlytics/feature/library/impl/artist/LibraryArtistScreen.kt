package com.catlytics.feature.library.impl.artist

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

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
    val selectedSectionIndex = rememberSaveable(content.summary.artist.id) { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = selectedSectionIndex.intValue,
        pageCount = { ArtistDetailSection.entries.size },
    )
    val coroutineScope = rememberCoroutineScope()
    val songsListState = rememberSaveable(
        content.summary.artist.id,
        saver = LazyListState.Saver,
    ) {
        LazyListState()
    }
    val albumsGridState = rememberSaveable(
        content.summary.artist.id,
        saver = LazyGridState.Saver,
    ) {
        LazyGridState()
    }
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

    LaunchedEffect(pagerState.currentPage) {
        selectedSectionIndex.intValue = pagerState.currentPage
    }

    LaunchedEffect(content.summary.artworkUri, artworkBitmap, fallbackGradient) {
        gradientColors = artworkBitmap?.extractArtworkGradientColors(fallbackGradient) ?: fallbackGradient
    }

    ArtworkGradientBackground(
        colors = animatedGradientColors,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            ArtistHeader(
                content = content,
                artworkModel = artworkRequest,
                onArtworkLoaded = { artworkBitmap = it },
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp,
                    bottom = 8.dp,
                ),
            )
            ArtistSectionTabs(
                selectedIndex = pagerState.currentPage,
                onSectionSelected = { index ->
                    selectedSectionIndex.intValue = index
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (ArtistDetailSection.entries[page]) {
                    ArtistDetailSection.Songs -> ArtistSongsPage(
                        tracks = content.tracks,
                        state = songsListState,
                        onTrackSelected = { track -> onTrackSelected(track, content.tracks) },
                        onTrackOptions = onTrackOptions,
                        bottomPadding = bottomPadding,
                    )
                    ArtistDetailSection.Albums -> ArtistAlbumsPage(
                        albums = content.albums,
                        state = albumsGridState,
                        onAlbumSelected = onAlbumSelected,
                        onAddToPlaylist = { album ->
                            onAddToPlaylist(PlaylistSource.AlbumSource(album.id))
                        },
                        bottomPadding = bottomPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    content: ArtistContent,
    artworkModel: Any?,
    onArtworkLoaded: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = artworkModel,
            contentDescription = "Imagen de ${content.summary.artist.name}",
            modifier = Modifier
                .fillMaxWidth(0.52f)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistSectionTabs(
    selectedIndex: Int,
    onSectionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecondaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(selectedIndex)
                    .padding(horizontal = 20.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                color = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        ArtistDetailSection.entries.forEachIndexed { index, section ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSectionSelected(index) },
                text = { Text(section.label) },
            )
        }
    }
}

@Composable
private fun ArtistSongsPage(
    tracks: List<Track>,
    state: LazyListState,
    onTrackSelected: (Track) -> Unit,
    onTrackOptions: (Track) -> Unit,
    bottomPadding: () -> Dp,
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 8.dp,
            end = 20.dp,
            bottom = bottomPadding() + 20.dp,
        ),
    ) {
        items(items = tracks, key = Track::id) { track ->
            ArtistTrackRow(
                track = track,
                onClick = { onTrackSelected(track) },
                onTrackOptions = { onTrackOptions(track) },
            )
        }
    }
}

@Composable
private fun ArtistAlbumsPage(
    albums: List<Album>,
    state: LazyGridState,
    onAlbumSelected: (Album) -> Unit,
    onAddToPlaylist: (Album) -> Unit,
    bottomPadding: () -> Dp,
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 16.dp,
            end = 20.dp,
            bottom = bottomPadding() + 20.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        gridItems(items = albums, key = Album::id) { album ->
            ArtistAlbumCard(
                album = album,
                onClick = { onAlbumSelected(album) },
                onAddToPlaylist = { onAddToPlaylist(album) },
            )
        }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.trackCount.trackCountLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(painterResource(R.drawable.ic_options), "Opciones de ${album.title}")
            }
        }
    }
}

@Composable
private fun ArtistTrackRow(
    track: Track,
    onClick: () -> Unit,
    onTrackOptions: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistTrackArtwork(track = track)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artist.name} · ${track.durationMillis.formatDuration()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onTrackOptions) {
            Icon(
                painter = painterResource(R.drawable.ic_options),
                contentDescription = "Opciones de ${track.title}",
            )
        }
    }
}

@Composable
private fun ArtistTrackArtwork(
    track: Track,
    modifier: Modifier = Modifier,
) {
    val artworkShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .blur(
                    radius = 8.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = artworkShape,
                ),
        )
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            placeholder = painterResource(R.drawable.placeholder_album),
            error = painterResource(R.drawable.placeholder_album),
            fallback = painterResource(R.drawable.placeholder_album),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(artworkShape),
        )
    }
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

private enum class ArtistDetailSection(val label: String) {
    Songs("Canciones"),
    Albums("Álbumes"),
}

private fun Int.trackCountLabel() = if (this == 1) "1 canción" else "$this canciones"

private fun Long.formatDuration(): String {
    val totalSeconds = milliseconds.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}


@Preview(name = "Phone", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun LibraryArtistScreenPhonePreview() {
    LibraryArtistScreenPreviewContent()
}

@Preview(name = "Tablet", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun LibraryArtistScreenTabletPreview() {
    LibraryArtistScreenPreviewContent()
}

@Composable
private fun LibraryArtistScreenPreviewContent() {
    CatlyticsTheme {
        LibraryArtistScreen(
            uiState = LibraryArtistUiState.Success(previewArtistContent()),
            onAlbumSelected = {},
            onTrackSelected = { _, _ -> },
            onAddToPlaylist = {},
            onTrackOptions = {},
            onTopBarColorChange = {},
        )
    }
}

private fun previewArtistContent(): ArtistContent {
    val artist = Artist("artist-preview", "Mitski")
    val albums = listOf(
        Album(
            id = "album-1",
            title = "The Land Is Inhospitable and So Are We",
            artist = artist,
            trackCount = 11,
        ),
        Album(
            id = "album-2",
            title = "Laurel Hell",
            artist = artist,
            trackCount = 11,
        ),
        Album(
            id = "album-3",
            title = "Be the Cowboy",
            artist = artist,
            trackCount = 14,
        ),
        Album(
            id = "album-4",
            title = "Puberty 2",
            artist = artist,
            trackCount = 11,
        ),
    )
    val tracks = listOf(
        Track(
            id = "track-1",
            title = "Bug Like an Angel",
            artist = artist,
            durationMillis = 212_000,
            mediaUri = "content://preview/track-1",
            albumTitle = albums[0].title,
        ),
        Track(
            id = "track-2",
            title = "Heaven",
            artist = artist,
            durationMillis = 224_000,
            mediaUri = "content://preview/track-2",
            albumTitle = albums[0].title,
        ),
        Track(
            id = "track-3",
            title = "Working for the Knife",
            artist = artist,
            durationMillis = 159_000,
            mediaUri = "content://preview/track-3",
            albumTitle = albums[1].title,
        ),
        Track(
            id = "track-4",
            title = "Nobody",
            artist = artist,
            durationMillis = 193_000,
            mediaUri = "content://preview/track-4",
            albumTitle = albums[2].title,
        ),
    )
    return ArtistContent(
        summary = ArtistSummary(
            artist = artist,
            albumCount = albums.size,
            trackCount = tracks.size,
        ),
        albums = albums,
        tracks = tracks,
    )
}
