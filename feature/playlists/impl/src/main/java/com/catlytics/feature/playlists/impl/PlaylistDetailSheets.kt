package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.model.Track
import com.catlytics.feature.playlists.impl.R as PlaylistsR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTracksToPlaylistSheet(
    tracks: List<Track>,
    existingTrackIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val filteredTracks = remember(tracks, query) {
        tracks
            .filterPlaylistTracksByQuery(query)
            .alphabeticalOrder()
    }

    fun toggle(trackId: String) {
        selectedIds = toggleTrackSelection(selectedIds, trackId, existingTrackIds)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(PlaylistsR.string.playlist_detail_add_tracks),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(PlaylistsR.string.playlist_detail_add_tracks_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(stringResource(PlaylistsR.string.playlist_detail_add_tracks_search_placeholder))
                },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                stringResource(PlaylistsR.string.playlists_clear_search_content_description),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CatlyticsCorners.Medium,
            )

            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (tracks.isEmpty()) {
                            stringResource(PlaylistsR.string.playlist_detail_add_tracks_empty)
                        } else {
                            stringResource(PlaylistsR.string.playlist_detail_add_tracks_no_search_results)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                ) {
                    items(filteredTracks, key = Track::id) { track ->
                        val alreadyAdded = track.id in existingTrackIds
                        val selected = track.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !alreadyAdded) { toggle(track.id) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = track.artworkUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CatlyticsCorners.Small),
                                placeholder = painterResource(R.drawable.placeholder_track),
                                error = painterResource(R.drawable.placeholder_track),
                                fallback = painterResource(R.drawable.placeholder_track),
                                contentScale = ContentScale.Crop,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (alreadyAdded) {
                                        stringResource(PlaylistsR.string.playlists_already_added)
                                    } else {
                                        track.artist.name
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Checkbox(
                                    checked = alreadyAdded || selected,
                                    onCheckedChange = { toggle(track.id) },
                                    enabled = !alreadyAdded,
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onAdd(tracks.selectedTrackIdsInLibraryOrder(selectedIds))
                },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (selectedIds.isEmpty()) {
                        stringResource(PlaylistsR.string.playlist_detail_add_tracks)
                    } else {
                        stringResource(PlaylistsR.string.playlist_detail_add_selected, selectedIds.size)
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditPlaylistSheet(
    name: String,
    description: String,
    artworkUri: String?,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onChooseArtwork: () -> Unit,
    onRemoveArtwork: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(PlaylistsR.string.playlist_detail_edit),
                style = MaterialTheme.typography.titleLarge,
            )
            PlaylistCover(
                playlistId = EDIT_PREVIEW_PLAYLIST_ID,
                name = name,
                artworkModel = artworkUri,
                mosaicArtworkUris = emptyList(),
                shape = CatlyticsCorners.Large,
                contentDescription = stringResource(
                    PlaylistsR.string.playlist_detail_artwork_preview_content_description,
                ),
                modifier = Modifier
                    .size(144.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onChooseArtwork) {
                    Text(stringResource(PlaylistsR.string.playlist_detail_change_artwork))
                }
                if (artworkUri != null) {
                    TextButton(onClick = onRemoveArtwork) {
                        Text(stringResource(PlaylistsR.string.playlist_detail_remove_artwork))
                    }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(PlaylistsR.string.playlists_name_label)) },
                singleLine = true,
                isError = name.isBlank(),
                supportingText = if (name.isBlank()) {
                    { Text(stringResource(PlaylistsR.string.playlist_detail_name_required)) }
                } else {
                    null
                },
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(PlaylistsR.string.playlist_detail_description_label)) },
                minLines = 3,
                maxLines = 5,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(PlaylistsR.string.playlists_action_cancel))
                }
                Button(onClick = onSave, enabled = name.isNotBlank()) {
                    Text(stringResource(PlaylistsR.string.playlists_action_save))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private const val EDIT_PREVIEW_PLAYLIST_ID = "edit-preview"
