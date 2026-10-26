package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsShuffleButton
import com.catlytics.core.designsystem.component.TrackListRow
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import com.catlytics.core.designsystem.layout.deferredPadding
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.home.impl.R as HomeR

internal const val MIN_SHUFFLE_TRACK_COUNT = 2

@Composable
internal fun TrackCollectionContent(
    tracks: List<Track>,
    currentTrackId: String?,
    isCurrentTrackPlaying: Boolean,
    summary: String,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    likedTrackIds: Set<String>,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    title: String? = null,
    badgeLabel: String? = null,
) {
    val selectionState = rememberTrackSelectionState()
    val selection = selectionState.value
    TrackSelectionHost(
        selection = selection,
        onSelectionChange = { selectionState.value = it },
        visibleIds = remember(tracks) { tracks.map(Track::id) },
        selectedTracks = remember(selection, tracks) { selection.selectedTracks(tracks) },
        likedTrackIds = likedTrackIds,
        currentTrackId = currentTrackId,
        topInset = { scaffoldContentPadding.calculateTopPadding() },
        bottomInset = bottomPadding,
        onAction = onTrackSelectionAction,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(CatlyticsLayoutTokens.ListItemMinColumnWidth),
            modifier = Modifier.fillMaxSize(),
            contentPadding = deferredPadding(
                top = {
                    scaffoldContentPadding.calculateTopPadding() +
                        if (selection.active) 64.dp else 0.dp
                },
                bottom = {
                    bottomPadding() + 20.dp +
                        if (selection.active) 72.dp else 0.dp
                },
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "track-collection-header", span = { GridItemSpan(maxLineSpan) }) {
                TrackCollectionHeader(
                    title = title,
                    summary = summary,
                    canShuffle = tracks.size >= MIN_SHUFFLE_TRACK_COUNT,
                    onShuffle = onShuffle,
                    onPlayAll = onPlayAll,
                )
            }
            items(items = tracks, key = Track::id) { track ->
                TrackListRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = track.id == currentTrackId && isCurrentTrackPlaying,
                    onClick = {
                        if (selection.active) {
                            selectionState.value = selection.onTrackLongClick(track.id)
                        } else {
                            onTrackSelected(track, tracks)
                        }
                    },
                    onOptions = { onTrackOptions(track) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    badgeLabel = badgeLabel,
                    selected = track.id in selection.selectedIds,
                    selectionActive = selection.active,
                    onLongClick = {
                        selectionState.value = selection.onTrackLongClick(track.id)
                    },
                )
            }
        }
    }
}

/** Con [title], el resumen va debajo en un estilo secundario; sin él, el resumen hace de título. */
@Composable
private fun TrackCollectionHeader(
    title: String?,
    summary: String,
    canShuffle: Boolean,
    onShuffle: () -> Unit,
    onPlayAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary,
                modifier = Modifier.weight(1f),
                style = if (title != null) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CatlyticsShuffleButton(onClick = onShuffle, enabled = canShuffle)
            FilledIconButton(
                onClick = onPlayAll,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = stringResource(
                        HomeR.string.track_collection_play_all_content_description,
                    ),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
internal fun TrackCollectionLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun TrackCollectionEmpty(message: String, modifier: Modifier = Modifier) {
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
