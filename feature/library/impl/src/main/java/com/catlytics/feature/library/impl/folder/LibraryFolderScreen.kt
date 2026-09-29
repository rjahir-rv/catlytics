package com.catlytics.feature.library.impl.folder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.designsystem.format.TrackDurationFormat
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.core.model.PlaylistSource
import com.catlytics.feature.library.impl.R as LibraryR
import androidx.compose.material3.IconButton

@Composable
internal fun LibraryFolderScreen(
    uiState: LibraryFolderUiState,
    modifier: Modifier = Modifier,
    onFolderSelected: (LibraryFolder) -> Unit,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    currentTrackId: String? = null,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
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
            message = stringResource(LibraryR.string.library_folder_not_found_message),
            modifier = modifier,
        )
        is LibraryFolderUiState.Error -> FolderMessage(uiState.message.asString(), modifier)
        is LibraryFolderUiState.Success -> {
            val content = uiState.content
            if (content.subfolders.isEmpty() && content.tracks.isEmpty()) {
                FolderMessage(
                    stringResource(LibraryR.string.library_folder_empty_message),
                    modifier,
                )
            } else {
                val selectionState = rememberTrackSelectionState()
                val selection = selectionState.value
                TrackSelectionHost(
                    selection = selection,
                    onSelectionChange = { selectionState.value = it },
                    visibleIds = content.tracks.map(Track::id),
                    selectedTracks = selection.selectedTracks(content.tracks),
                    likedTrackIds = likedTrackIds,
                    currentTrackId = currentTrackId,
                    topInset = scaffoldContentPadding.calculateTopPadding(),
                    bottomInset = bottomPadding(),
                    onAction = onTrackSelectionAction,
                    modifier = modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            top = scaffoldContentPadding.calculateTopPadding() + 20.dp +
                                if (selection.active) 64.dp else 0.dp,
                            end = 20.dp,
                            bottom = bottomPadding() + 20.dp +
                                if (selection.active) 72.dp else 0.dp,
                        ),
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
                                onClick = {
                                    if (selection.active) {
                                        selectionState.value = selection.onTrackLongClick(track.id)
                                    } else {
                                        onTrackSelected(track, content.tracks)
                                    }
                                },
                                onLongClick = {
                                    selectionState.value = selection.onTrackLongClick(track.id)
                                },
                                onTrackOptions = { onTrackOptions(track) },
                                selected = track.id in selection.selectedIds,
                                selectionActive = selection.active,
                            )
                            HorizontalDivider()
                        }
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
            Icon(
                painterResource(R.drawable.ic_options),
                stringResource(
                    LibraryR.string.library_folder_options_content_description,
                    folder.name,
                ),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    LibraryR.plurals.library_track_count,
                    folder.trackCount,
                    folder.trackCount,
                ),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    onTrackOptions: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectionActive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionActive) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_list),
                        contentDescription = stringResource(
                            LibraryR.string.library_track_selected_content_description,
                        ),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(3.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = track.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = track.artist.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = TrackDurationFormat.format(track.durationMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!selectionActive) {
            IconButton(onClick = onTrackOptions) {
                Icon(
                    painterResource(R.drawable.ic_options),
                    stringResource(
                        LibraryR.string.library_track_options_content_description,
                        track.title,
                    ),
                )
            }
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
