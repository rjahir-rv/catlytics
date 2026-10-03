package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsPlayingBarsOverlay
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.model.PlaylistSummary
import com.catlytics.feature.playlists.impl.R as PlaylistsR

/** Fila del modo lista: portada, nombre, resumen y acciones. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlaylistListRow(
    summary: PlaylistSummary,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist = summary.playlist
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(CatlyticsCorners.Large)
            .background(containerColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onLongClickLabel = stringResource(
                    PlaylistsR.string.playlists_options_content_description,
                    playlist.displayName(),
                ),
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistCoverWithPlayback(
            summary = summary,
            showPlaying = isActive && isPlaying,
            shape = CatlyticsCorners.Medium,
            modifier = Modifier.size(LIST_COVER_SIZE),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = playlist.displayName(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playlistSummaryLabel(summary.trackCount, summary.totalDurationMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PlaylistOptionsButton(playlistName = playlist.displayName(), onClick = onOptionsClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlaylistGridCard(
    summary: PlaylistSummary,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist = summary.playlist
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(CatlyticsCorners.XLarge)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onLongClickLabel = stringResource(
                    PlaylistsR.string.playlists_options_content_description,
                    playlist.displayName(),
                ),
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            PlaylistCoverWithPlayback(
                summary = summary,
                showPlaying = isActive && isPlaying,
                shape = CatlyticsCorners.XLarge,
                modifier = Modifier.fillMaxSize(),
            )
            if (summary.trackCount > 0) {
                val playing = isActive && isPlaying
                val playDescription = stringResource(
                    PlaylistsR.string.playlists_play_content_description,
                    playlist.displayName(),
                )
                val pauseDescription = stringResource(
                    PlaylistsR.string.playlist_detail_pause_content_description,
                )
                FilledIconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(GRID_PLAY_BUTTON_SIZE),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        painter = painterResource(
                            if (playing) R.drawable.ic_pause_fill else R.drawable.ic_play_fill,
                        ),
                        contentDescription = if (playing) pauseDescription else playDescription,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playlistSummaryLabel(summary.trackCount, summary.totalDurationMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PlaylistOptionsButton(playlistName = playlist.displayName(), onClick = onOptionsClick)
        }
    }
}

@Composable
internal fun PlaylistCoverWithPlayback(
    summary: PlaylistSummary,
    showPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val playlist = summary.playlist
    Box(modifier = modifier) {
        PlaylistCover(
            playlistId = playlist.id,
            name = playlist.name,
            artworkModel = playlist.artworkUri,
            mosaicArtworkUris = summary.coverArtworkUris,
            shape = shape,
            modifier = Modifier.fillMaxSize(),
        )
        if (showPlaying) {
            CatlyticsPlayingBarsOverlay(
                contentDescription = stringResource(
                    PlaylistsR.string.playlists_now_playing_content_description,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
            )
        }
    }
}

@Composable
private fun PlaylistOptionsButton(
    playlistName: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_options),
            contentDescription = stringResource(
                PlaylistsR.string.playlists_options_content_description,
                playlistName,
            ),
        )
    }
}

private val LIST_COVER_SIZE = 64.dp
private val GRID_PLAY_BUTTON_SIZE = 40.dp
