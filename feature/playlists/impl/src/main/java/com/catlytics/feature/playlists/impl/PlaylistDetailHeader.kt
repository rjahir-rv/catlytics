package com.catlytics.feature.playlists.impl

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.domain.usecase.playlist.PLAYLIST_COVER_MOSAIC_SIZE
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.Track
import com.catlytics.core.model.distinctArtworkUris
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
    onAddTracksClick: () -> Unit,
    onOptionsClick: () -> Unit,
    optionsMenu: @Composable () -> Unit,
    onCancelOrdering: () -> Unit,
    onSaveOrdering: () -> Unit,
) {
    val coverShape = CatlyticsCorners.XLarge
    val mosaicArtworkUris = remember(tracks) { tracks.distinctArtworkUris(PLAYLIST_COVER_MOSAIC_SIZE) }
    val entrance = remember(playlist.id) { Animatable(0f) }
    LaunchedEffect(playlist.id) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val coverSize = (maxWidth * 0.64f).coerceAtMost(240.dp)
            PlaylistCover(
                playlistId = playlist.id,
                name = playlist.name,
                artworkModel = if (playlist.artworkUri != null) artworkRequest else null,
                mosaicArtworkUris = mosaicArtworkUris,
                shape = coverShape,
                contentDescription = stringResource(
                    PlaylistsR.string.playlists_artwork_content_description,
                    playlist.displayName(),
                ),
                onArtworkLoaded = onArtworkLoaded,
                modifier = Modifier
                    .size(coverSize)
                    .graphicsLayer {
                        val progress = entrance.value
                        alpha = progress.coerceIn(0f, 1f)
                        scaleX = 0.9f + 0.1f * progress
                        scaleY = 0.9f + 0.1f * progress
                    }
                    .shadow(elevation = 20.dp, shape = coverShape),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = playlist.displayName(),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (playlist.description.isNotBlank()) {
                Text(
                    text = playlist.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        PlaylistMetaPills(tracks = tracks)

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
                onAddTracksClick = onAddTracksClick,
                onOptionsClick = onOptionsClick,
                optionsMenu = optionsMenu,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaylistMetaPills(tracks: List<Track>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetaPill(
            pluralStringResource(
                PlaylistsR.plurals.playlists_track_count,
                tracks.size,
                tracks.size,
            ),
        )
        val durationMillis = tracks.sumOf(Track::durationMillis)
        if (durationMillis > 0) {
            MetaPill(formatPlaylistTotalDuration(durationMillis))
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
    onAddTracksClick: () -> Unit,
    onOptionsClick: () -> Unit,
    optionsMenu: @Composable () -> Unit,
) {
    val source = playbackState.queueSource
    val isThisPlaylistActive = source is PlaybackQueueSource.Playlist &&
        source.playlistId == playlist.id
    val isPlayingThis = isThisPlaylistActive &&
        (playbackState.status == PlaybackStatus.Playing ||
            playbackState.status == PlaybackStatus.Buffering)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Button(
            onClick = {
                if (isThisPlaylistActive) {
                    onTogglePlayback()
                } else {
                    tracks.firstOrNull()?.let { onPlay(it, tracks) }
                }
            },
            enabled = tracks.isNotEmpty(),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (isPlayingThis) R.drawable.ic_pause_fill else R.drawable.ic_play_fill,
                ),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(
                    if (isPlayingThis) R.string.ds_action_pause else R.string.ds_action_play,
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        FilledTonalIconButton(
            onClick = onAddTracksClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_add),
                contentDescription = stringResource(PlaylistsR.string.playlist_detail_add_tracks),
                modifier = Modifier.size(24.dp),
            )
        }
        Box {
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
}

/** Marcador de posición del encabezado mientras carga la playlist. */
@Composable
internal fun PlaylistDetailSkeleton(
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "playlistDetailSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "playlistDetailSkeletonAlpha",
    )
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = topPadding + 12.dp)
            .graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(200.dp)
                .clip(CatlyticsCorners.XLarge)
                .background(block),
        )
        Box(
            Modifier
                .width(180.dp)
                .height(24.dp)
                .clip(CircleShape)
                .background(block),
        )
        Box(
            Modifier
                .width(120.dp)
                .height(16.dp)
                .clip(CircleShape)
                .background(block),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(block),
        )
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
