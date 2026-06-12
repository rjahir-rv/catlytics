package com.catlytics.feature.library.impl.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.Track
import com.catlytics.core.model.PlaylistSource
import androidx.compose.material3.IconButton
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun LibraryFolderScreen(
    uiState: LibraryFolderUiState,
    onFolderSelected: (LibraryFolder) -> Unit,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        LibraryFolderUiState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        LibraryFolderUiState.NotFound -> FolderMessage(
            message = "Esta carpeta ya no está disponible.",
            modifier = modifier,
        )
        is LibraryFolderUiState.Error -> FolderMessage(uiState.message, modifier)
        is LibraryFolderUiState.Success -> {
            val content = uiState.content
            if (content.subfolders.isEmpty() && content.tracks.isEmpty()) {
                FolderMessage("Esta carpeta no contiene música.", modifier)
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                ) {
                    items(content.subfolders, key = LibraryFolder::id) { folder ->
                        SubfolderRow(
                            folder = folder,
                            onClick = { onFolderSelected(folder) },
                            onAddToPlaylist = {
                                onAddToPlaylist(PlaylistSource.FolderSource(folder.id))
                            },
                        )
                        HorizontalDivider()
                    }
                    items(content.tracks, key = Track::id) { track ->
                        TrackRow(
                            track = track,
                            onClick = { onTrackSelected(track, content.tracks) },
                            onAddToPlaylist = {
                                onAddToPlaylist(PlaylistSource.TrackSource(track.id))
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SubfolderRow(folder: LibraryFolder, onClick: () -> Unit, onAddToPlaylist: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onAddToPlaylist) {
            Icon(painterResource(R.drawable.ic_options), "Opciones de ${folder.name}")
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = folder.trackCount.trackCountLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
        )
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit, onAddToPlaylist: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = track.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = track.artist.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = track.durationMillis.formatDuration(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onAddToPlaylist) {
            Icon(painterResource(R.drawable.ic_options), "Opciones de ${track.title}")
        }
    }
}

@Composable
private fun FolderMessage(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
