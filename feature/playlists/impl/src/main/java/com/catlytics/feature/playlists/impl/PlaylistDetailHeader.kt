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
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.catlytics.feature.playlists.impl.R as PlaylistsR

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
                    contentDescription = stringResource(
                        PlaylistsR.string.playlists_artwork_content_description,
                        playlist.displayName(),
                    ),
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
                        contentDescription = stringResource(
                            PlaylistsR.string.playlist_detail_options_content_description,
                        ),
                    )
                }
                optionsMenu()
            }
        }

        Text(
            playlist.displayName(),
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
                    stringResource(PlaylistsR.string.playlist_detail_ordering_hint),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onCancelOrdering) {
                    Text(stringResource(PlaylistsR.string.playlists_action_cancel))
                }
                Button(onClick = onSaveOrdering) {
                    Text(stringResource(PlaylistsR.string.playlists_action_save))
                }
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
            playlistSummaryLabel(tracks),
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
                painterResource(R.drawable.ic_shuffle),
                contentDescription = stringResource(
                    PlaylistsR.string.playlist_detail_shuffle_content_description,
                ),
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
                contentDescription = stringResource(
                    if (isPlayingThis) {
                        PlaylistsR.string.playlist_detail_pause_content_description
                    } else {
                        PlaylistsR.string.playlist_detail_play_content_description
                    },
                ),
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
    onExportM3u: () -> Unit = {},
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(PlaylistsR.string.playlist_detail_order)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_filter), null) },
            onClick = { onDismiss(); onOrder() },
        )
        if (canEdit) {
            DropdownMenuItem(
                text = { Text(stringResource(PlaylistsR.string.playlist_detail_edit)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null) },
                onClick = { onDismiss(); onEdit() },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(PlaylistsR.string.playlist_detail_add_tracks)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_add), null) },
            onClick = { onDismiss(); onAddTracks() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(PlaylistsR.string.playlist_detail_add_to_playlist)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_add_playlist), null) },
            onClick = { onDismiss(); onAddToPlaylist() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(PlaylistsR.string.playlist_detail_export_m3u8)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_share), null) },
            onClick = { onDismiss(); onExportM3u() },
        )
        if (canEdit) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(PlaylistsR.string.playlist_detail_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
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
    val optionColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(PlaylistsR.string.playlist_detail_order),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        ListItem(
            headlineContent = { Text(stringResource(PlaylistsR.string.playlist_detail_order_custom)) },
            supportingContent = {
                Text(stringResource(PlaylistsR.string.playlist_detail_order_custom_hint))
            },
            colors = optionColors,
            modifier = Modifier.clickable(onClick = onCustom),
        )
        ListItem(
            headlineContent = {
                Text(stringResource(PlaylistsR.string.playlist_detail_order_alphabetical))
            },
            supportingContent = {
                Text(stringResource(PlaylistsR.string.playlist_detail_order_alphabetical_hint))
            },
            colors = optionColors,
            modifier = Modifier.clickable(onClick = onAlphabetical),
        )
        ListItem(
            headlineContent = { Text(stringResource(PlaylistsR.string.playlist_detail_order_random)) },
            supportingContent = {
                Text(stringResource(PlaylistsR.string.playlist_detail_order_random_hint))
            },
            colors = optionColors,
            modifier = Modifier.clickable(onClick = onRandom),
        )
        Spacer(Modifier.height(24.dp))
    }
}
