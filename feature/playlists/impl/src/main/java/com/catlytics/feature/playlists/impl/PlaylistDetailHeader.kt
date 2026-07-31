package com.catlytics.feature.playlists.impl

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.toBitmap
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.Track

@Composable
internal fun PlaylistHeader(
    playlist: Playlist,
    tracks: List<Track>,
    artworkRequest: ImageRequest,
    playbackState: PlaybackState,
    customOrdering: Boolean,
    onArtworkLoaded: (Bitmap) -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onPlayShuffled: (List<Track>) -> Unit,
    onTogglePlayback: () -> Unit,
    onOptionsClick: () -> Unit,
    optionsMenu: @Composable () -> Unit,
    onCancelOrdering: () -> Unit,
    onSaveOrdering: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val artworkSize = (maxWidth * 0.72f).coerceAtMost(280.dp)
                val coverPlaceholder = painterResource(
                    if (playlist.id == LIKED_PLAYLIST_ID) {
                        R.drawable.placeholder_favorites
                    } else {
                        R.drawable.placeholder_playlist
                    },
                )
                AsyncImage(
                    model = artworkRequest,
                    contentDescription = "Portada de ${playlist.name}",
                    modifier = Modifier
                        .size(artworkSize)
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = coverPlaceholder,
                    error = coverPlaceholder,
                    fallback = coverPlaceholder,
                    onSuccess = { state -> onArtworkLoaded(state.result.image.toBitmap()) },
                    contentScale = ContentScale.Crop,
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = onOptionsClick) {
                    Icon(
                        painterResource(R.drawable.ic_options),
                        contentDescription = "Opciones de la playlist",
                    )
                }
                optionsMenu()
            }
        }

        Text(
            playlist.name,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (playlist.description.isNotBlank()) {
            Text(
                playlist.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (customOrdering) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Mantén pulsado el control y arrastra",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onCancelOrdering) { Text("Cancelar") }
                Button(onClick = onSaveOrdering) { Text("Guardar") }
            }
        } else {
            PlaylistPlaybackActions(
                playlist = playlist,
                tracks = tracks,
                playbackState = playbackState,
                onPlay = onPlay,
                onPlayShuffled = onPlayShuffled,
                onTogglePlayback = onTogglePlayback,
            )
        }
    }
}

@Composable
private fun PlaylistPlaybackActions(
    playlist: Playlist,
    tracks: List<Track>,
    playbackState: PlaybackState,
    onPlay: (Track, List<Track>) -> Unit,
    onPlayShuffled: (List<Track>) -> Unit,
    onTogglePlayback: () -> Unit,
) {
    val source = playbackState.queueSource
    val isThisPlaylistActive = source is PlaybackQueueSource.Playlist &&
        source.playlistId == playlist.id
    val isPlayingThis = isThisPlaylistActive &&
        (playbackState.status == PlaybackStatus.Playing ||
            playbackState.status == PlaybackStatus.Buffering)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (tracks.size == 1) "1 canción" else "${tracks.size} canciones",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalIconButton(
            onClick = { onPlayShuffled(tracks) },
            enabled = tracks.isNotEmpty(),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_shuffle_square),
                contentDescription = "Reproducir aleatoriamente",
                modifier = Modifier.size(24.dp),
            )
        }
        FilledIconButton(
            onClick = {
                if (isThisPlaylistActive) {
                    onTogglePlayback()
                } else {
                    tracks.firstOrNull()?.let { onPlay(it, tracks) }
                }
            },
            enabled = tracks.isNotEmpty(),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (isPlayingThis) R.drawable.ic_pause else R.drawable.ic_play,
                ),
                contentDescription = if (isPlayingThis) "Pausar" else "Reproducir playlist",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
internal fun PlaylistOptionsMenu(
    expanded: Boolean,
    canEdit: Boolean,
    onDismiss: () -> Unit,
    onOrder: () -> Unit,
    onEdit: () -> Unit,
    onAddTracks: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Ordenar canciones") },
            leadingIcon = { Icon(painterResource(R.drawable.ic_filter), null) },
            onClick = { onDismiss(); onOrder() },
        )
        if (canEdit) {
            DropdownMenuItem(
                text = { Text("Editar playlist") },
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null) },
                onClick = { onDismiss(); onEdit() },
            )
        }
        DropdownMenuItem(
            text = { Text("Agregar canciones") },
            leadingIcon = { Icon(painterResource(R.drawable.ic_add), null) },
            onClick = { onDismiss(); onAddTracks() },
        )
        DropdownMenuItem(
            text = { Text("Agregar a otra playlist") },
            leadingIcon = { Icon(painterResource(R.drawable.ic_add_playlist), null) },
            onClick = { onDismiss(); onAddToPlaylist() },
        )
        if (canEdit) {
            DropdownMenuItem(
                text = { Text("Eliminar playlist", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = { onDismiss(); onDelete() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistOrderSheet(
    onDismiss: () -> Unit,
    onCustom: () -> Unit,
    onAlphabetical: () -> Unit,
    onRandom: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Ordenar canciones",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        ListItem(
            headlineContent = { Text("Personalizado") },
            supportingContent = { Text("Arrastra las canciones al orden que prefieras") },
            modifier = Modifier.clickable(onClick = onCustom),
        )
        ListItem(
            headlineContent = { Text("Alfabético") },
            supportingContent = { Text("Ordenar por título y artista") },
            modifier = Modifier.clickable(onClick = onAlphabetical),
        )
        ListItem(
            headlineContent = { Text("Random") },
            supportingContent = { Text("Mezclar y guardar un orden nuevo") },
            modifier = Modifier.clickable(onClick = onRandom),
        )
        Spacer(Modifier.height(24.dp))
    }
}
