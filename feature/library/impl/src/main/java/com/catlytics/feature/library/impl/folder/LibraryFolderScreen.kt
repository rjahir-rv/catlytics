package com.catlytics.feature.library.impl.folder

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import com.catlytics.core.designsystem.component.CatlyticsTrackRow
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.designsystem.format.TrackDurationFormat
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Artist
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.library.impl.LibraryDimens
import com.catlytics.feature.library.impl.R as LibraryR
import com.catlytics.feature.library.impl.root.FolderIcon

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
                            start = LibraryDimens.ScreenPadding,
                            top = scaffoldContentPadding.calculateTopPadding() + 12.dp +
                                if (selection.active) 64.dp else 0.dp,
                            end = LibraryDimens.ScreenPadding,
                            bottom = bottomPadding() + LibraryDimens.ContentBottomSpacing +
                                if (selection.active) 72.dp else 0.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item(key = "folder-header") {
                            FolderHeader(
                                content = content,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        items(content.subfolders, key = LibraryFolder::id) { folder ->
                            SubfolderRow(
                                folder = folder,
                                onClick = { onFolderSelected(folder) },
                                onAddToPlaylist = {
                                    onAddToPlaylist(PlaylistSource.FolderSource(folder.id))
                                },
                            )
                        }
                        items(content.tracks, key = Track::id) { track ->
                            val isCurrent = track.id == currentTrackId
                            CatlyticsTrackRow(
                                title = track.title,
                                subtitle = stringResource(
                                    LibraryR.string.library_track_metadata,
                                    track.artist.name,
                                    TrackDurationFormat.format(track.durationMillis),
                                ),
                                artworkUri = track.artworkUri,
                                isCurrent = isCurrent,
                                isPlaying = false,
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
                                selected = track.id in selection.selectedIds,
                                selectionActive = selection.active,
                                trailing = {
                                    IconButton(onClick = { onTrackOptions(track) }) {
                                        Icon(
                                            painterResource(R.drawable.ic_options),
                                            stringResource(
                                                LibraryR.string.library_track_options_content_description,
                                                track.title,
                                            ),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderHeader(
    content: LibraryFolderContent,
    modifier: Modifier = Modifier,
) {
    val counts = buildList {
        if (content.subfolders.isNotEmpty()) {
            add(
                pluralStringResource(
                    LibraryR.plurals.library_subfolder_count,
                    content.subfolders.size,
                    content.subfolders.size,
                ),
            )
        }
        if (content.tracks.isNotEmpty()) {
            add(
                pluralStringResource(
                    LibraryR.plurals.library_track_count,
                    content.tracks.size,
                    content.tracks.size,
                ),
            )
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = content.folder.path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = counts.joinToString(separator = " · "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SubfolderRow(
    folder: LibraryFolder,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(LibraryDimens.RowShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderIcon(isVisible = folder.isVisible)
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                painterResource(R.drawable.ic_options),
                stringResource(
                    LibraryR.string.library_folder_options_content_description,
                    folder.name,
                ),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FolderMessage(message: String, modifier: Modifier = Modifier) {
    CatlyticsEmptyState(
        message = message,
        modifier = modifier,
        messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Phone", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun LibraryFolderScreenPreview() {
    CatlyticsTheme {
        LibraryFolderScreen(
            uiState = LibraryFolderUiState.Success(
                LibraryFolderContent(
                    folder = LibraryFolder(
                        id = "external:Music",
                        name = "Music",
                        path = "Music",
                        trackCount = 3,
                        isVisible = true,
                    ),
                    subfolders = listOf(
                        LibraryFolder(
                            id = "external:Music/Live",
                            name = "Live",
                            path = "Music/Live",
                            trackCount = 12,
                            isVisible = true,
                        ),
                    ),
                    tracks = listOf(
                        Track(
                            id = "track-1",
                            title = "Midnight Signals",
                            artist = Artist("artist-1", "Catlytics"),
                            durationMillis = 215_000,
                            mediaUri = "content://media/track-1",
                        ),
                    ),
                ),
            ),
            onFolderSelected = {},
            onTrackSelected = { _, _ -> },
            onAddToPlaylist = {},
            onTrackOptions = {},
            currentTrackId = "track-1",
        )
    }
}
