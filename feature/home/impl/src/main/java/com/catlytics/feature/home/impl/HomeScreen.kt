package com.catlytics.feature.home.impl

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun HomeRoute(
    searchQuery: String,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permission = rememberRequiredAudioPermission()
    var hasAudioPermission by remember(permission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasAudioPermission = isGranted
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.refreshLibraryOnce()
        }
    }

    HomeScreen(
        uiState = uiState,
        searchQuery = searchQuery,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = { permissionLauncher.launch(permission) },
        onTrackSelected = viewModel::onTrackSelected,
        onAddToPlaylist = onAddToPlaylist,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    searchQuery: String,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!hasAudioPermission) {
            PermissionRequiredContent(onRequestPermission = onRequestPermission)
            return@Column
        }

        when (uiState) {
            HomeUiState.Empty -> EmptyLibraryContent()
            is HomeUiState.Error -> ErrorContent(message = uiState.message)
            HomeUiState.Loading -> LoadingContent()
            is HomeUiState.Success -> {
                val filteredTracks = uiState.tracks.filterByQuery(searchQuery)
                if (filteredTracks.isEmpty() && searchQuery.isNotBlank()) {
                    NoSearchResultsContent()
                } else {
                    TrackList(
                        tracks = filteredTracks,
                        currentTrackId = uiState.currentTrackId,
                        onTrackSelected = onTrackSelected,
                        state = trackListState,
                        onAddToPlaylist = onAddToPlaylist,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberRequiredAudioPermission(): String = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Catlytics necesita permiso para leer tu musica local.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(onClick = onRequestPermission) {
            Text(text = "Permitir acceso a musica")
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyLibraryContent(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = "No encontramos canciones en este dispositivo.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun NoSearchResultsContent(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = "No encontramos canciones que coincidan con tu búsqueda.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    currentTrackId: String?,
    onTrackSelected: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    onAddToPlaylist: (PlaylistSource) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = tracks,
            key = { it.id },
        ) { track ->
            TrackRow(
                track = track,
                isCurrent = track.id == currentTrackId,
                onTrackSelected = { onTrackSelected(track, tracks) },
                onAddToPlaylist = { onAddToPlaylist(PlaylistSource.TrackSource(track.id)) },
            )
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onTrackSelected: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onTrackSelected)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(
            track = track,
            isCurrent = isCurrent,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
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
        Icon(
            painter = painterResource(R.drawable.ic_favorite),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                painter = painterResource(R.drawable.ic_options),
                contentDescription = "Opciones de ${track.title}",
            )
        }
    }
}

@Composable
private fun TrackArtwork(
    track: Track,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    val artworkShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .blur(
                        radius = 16.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        shape = artworkShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .blur(
                        radius = 8.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = artworkShape,
                    ),
            )
        }
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

private fun Long.formatDuration(): String {
    val duration = milliseconds
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    CatlyticsTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                tracks = listOf(
                    Track(
                        id = "track-current",
                        title = "Electric Feel",
                        artist = Artist(
                            id = "artist-preview",
                            name = "MGMT",
                        ),
                        durationMillis = 186_000,
                        mediaUri = "content://media/external/audio/media/1",
                    ),
                    Track(
                        id = "track-preview",
                        title = "Canción local con un título bastante largo",
                        artist = Artist(
                            id = "artist-local",
                            name = "Artista local",
                        ),
                        durationMillis = 242_000,
                        mediaUri = "content://media/external/audio/media/2",
                    ),
                ),
                currentTrackId = "track-current",
            ),
            searchQuery = "",
            hasAudioPermission = true,
            onRequestPermission = {},
            onTrackSelected = { _, _ -> },
            onAddToPlaylist = {},
        )
    }
}
