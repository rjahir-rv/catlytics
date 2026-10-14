package com.catlytics.feature.settings.impl.edits

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import com.catlytics.core.designsystem.component.CatlyticsTrackRow
import com.catlytics.core.designsystem.text.resolve
import com.catlytics.feature.settings.impl.R as SettingsR
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun MetadataEditsRoute(
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: MetadataEditsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message.resolve(resources), Toast.LENGTH_SHORT).show()
        }
    }

    MetadataEditsContent(
        state = uiState,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onToggleSelection = viewModel::toggleSelection,
        onSelectAll = viewModel::selectAllVisible,
        onClearSelection = viewModel::clearSelection,
        onRevertField = viewModel::revertField,
        onRevertTrack = viewModel::revertTrack,
        onRevertArtist = viewModel::revertArtist,
        onRevertSelected = viewModel::requestRevertSelected,
        onRevertFiltered = viewModel::requestRevertFiltered,
        onConfirmDialog = viewModel::confirmDialog,
        onDismissDialog = viewModel::dismissDialog,
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
        modifier = modifier,
    )
}

@Composable
internal fun MetadataEditsContent(
    state: MetadataEditsUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (MetadataEditFilter) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRevertField: (String, EditedField) -> Unit,
    onRevertTrack: (String) -> Unit,
    onRevertArtist: (com.catlytics.core.model.ArtistAlias) -> Unit,
    onRevertSelected: () -> Unit,
    onRevertFiltered: () -> Unit,
    onConfirmDialog: () -> Unit,
    onDismissDialog: () -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (!state.hasAnyEdits) {
        CatlyticsEmptyState(
            title = stringResource(SettingsR.string.settings_edits_empty_title),
            message = stringResource(SettingsR.string.settings_edits_empty_message),
            modifier = modifier.padding(top = scaffoldContentPadding.calculateTopPadding()),
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = scaffoldContentPadding.calculateTopPadding() + 16.dp,
            end = 20.dp,
            bottom = bottomPadding() + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "description") {
            Text(
                text = stringResource(SettingsR.string.settings_edits_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item(key = "search") {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(SettingsR.string.settings_edits_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetadataEditFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = filter == state.filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(stringResource(filter.labelRes)) },
                    )
                }
            }
        }
        item(key = "actions") {
            BulkActionsRow(
                state = state,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                onRevertSelected = onRevertSelected,
                onRevertFiltered = onRevertFiltered,
            )
        }
        if (!state.hasResults) {
            item(key = "no-results") {
                Text(
                    text = stringResource(SettingsR.string.settings_edits_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
        if (state.tracks.isNotEmpty()) {
            item(key = "tracks-header") {
                SectionHeader(
                    pluralStringResource(
                        SettingsR.plurals.settings_edits_tracks_section,
                        state.tracks.size,
                        state.tracks.size,
                    ),
                )
            }
            items(state.tracks, key = { "track-${it.trackId}" }) { item ->
                TrackEditRow(
                    item = item,
                    selected = item.trackId in state.selectedTrackIds,
                    selectionActive = state.selectionActive,
                    enabled = !state.isBusy,
                    onToggleSelection = { onToggleSelection(item.trackId) },
                    onRevertField = { field -> onRevertField(item.trackId, field) },
                    onRevertTrack = { onRevertTrack(item.trackId) },
                )
            }
        }
        if (state.artists.isNotEmpty()) {
            item(key = "artists-header") {
                SectionHeader(
                    pluralStringResource(
                        SettingsR.plurals.settings_edits_artists_section,
                        state.artists.size,
                        state.artists.size,
                    ),
                )
            }
            items(state.artists, key = { "artist-${it.alias.source.name}" }) { item ->
                ArtistEditRow(
                    item = item,
                    enabled = !state.isBusy && !state.selectionActive,
                    onRevert = { onRevertArtist(item.alias) },
                )
            }
        }
    }

    MetadataEditsDialogs(
        dialog = state.dialog,
        isBusy = state.isBusy,
        onConfirm = onConfirmDialog,
        onDismiss = onDismissDialog,
    )
}

@Composable
private fun BulkActionsRow(
    state: MetadataEditsUiState,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onRevertSelected: () -> Unit,
    onRevertFiltered: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.selectionActive) {
            Text(
                text = pluralStringResource(
                    SettingsR.plurals.settings_edits_selected,
                    state.selectedTrackIds.size,
                    state.selectedTrackIds.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSelectAll, enabled = !state.isBusy) {
                Text(stringResource(SettingsR.string.settings_edits_select_all))
            }
            TextButton(onClick = onClearSelection, enabled = !state.isBusy) {
                Text(stringResource(SettingsR.string.settings_edits_cancel))
            }
            Button(onClick = onRevertSelected, enabled = !state.isBusy) {
                Text(stringResource(SettingsR.string.settings_edits_revert))
            }
        } else {
            Text(
                text = stringResource(SettingsR.string.settings_edits_selection_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRevertFiltered, enabled = state.hasResults && !state.isBusy) {
                Text(stringResource(SettingsR.string.settings_edits_revert_filtered))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun TrackEditRow(
    item: TrackEditItem,
    selected: Boolean,
    selectionActive: Boolean,
    enabled: Boolean,
    onToggleSelection: () -> Unit,
    onRevertField: (EditedField) -> Unit,
    onRevertTrack: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val subtitle = if (item.inLibrary) {
        item.artistName
    } else {
        stringResource(SettingsR.string.settings_edits_missing_track, item.artistName)
    }
    Column {
        CatlyticsTrackRow(
            title = item.title,
            subtitle = subtitle,
            artworkUri = item.artworkUri,
            isCurrent = false,
            isPlaying = false,
            onClick = { if (selectionActive) onToggleSelection() else menuExpanded = true },
            onLongClick = onToggleSelection,
            selected = selected,
            selectionActive = selectionActive,
            trailing = {
                Box {
                    IconButton(onClick = { menuExpanded = true }, enabled = enabled) {
                        Icon(
                            painter = painterResource(R.drawable.ic_options),
                            contentDescription = stringResource(
                                SettingsR.string.settings_edits_options_content_description,
                                item.title,
                            ),
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        // Single fields can only be restored for tracks still in the library.
                        if (item.inLibrary && item.changes.size > 1) {
                            item.changes.forEach { change ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(change.field.restoreLabelRes)) },
                                    onClick = {
                                        menuExpanded = false
                                        onRevertField(change.field)
                                    },
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(SettingsR.string.settings_edits_restore_track)) },
                            onClick = {
                                menuExpanded = false
                                onRevertTrack()
                            },
                        )
                    }
                }
            },
        )
        Column(
            modifier = Modifier.padding(start = 76.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item.changes.forEach { change -> ChangeLine(change) }
            Text(
                text = stringResource(SettingsR.string.settings_edits_updated_at, item.updatedAtMillis.formatDate()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChangeLine(change: FieldChange) {
    val label = stringResource(change.field.labelRes)
    val text = if (change.field == EditedField.Artwork) {
        stringResource(SettingsR.string.settings_edits_artwork_changed)
    } else {
        stringResource(
            SettingsR.string.settings_edits_change,
            change.original?.ifBlank { null } ?: "—",
            change.edited.orEmpty(),
        )
    }
    Text(
        text = "$label: $text",
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ArtistEditRow(
    item: ArtistEditItem,
    enabled: Boolean,
    onRevert: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(item.alias.source.name) },
        supportingContent = {
            Text(
                stringResource(
                    when (item.kind) {
                        ArtistEditKind.Merge -> SettingsR.string.settings_edits_artist_merged
                        ArtistEditKind.Rename -> SettingsR.string.settings_edits_artist_renamed
                        ArtistEditKind.Spelling -> SettingsR.string.settings_edits_artist_spelling
                    },
                    item.alias.target.name,
                ),
            )
        },
        trailingContent = {
            TextButton(onClick = onRevert, enabled = enabled) {
                Text(
                    stringResource(
                        if (item.kind == ArtistEditKind.Merge) {
                            SettingsR.string.settings_edits_separate
                        } else {
                            SettingsR.string.settings_edits_restore_name
                        },
                    ),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun MetadataEditsDialogs(
    dialog: MetadataEditsDialog,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = when (dialog) {
        MetadataEditsDialog.Hidden -> return
        is MetadataEditsDialog.ConfirmRevertSelected -> pluralStringResource(
            SettingsR.plurals.settings_edits_confirm_selected,
            dialog.trackCount,
            dialog.trackCount,
        )
        is MetadataEditsDialog.ConfirmRevertFiltered -> {
            val tracks = pluralStringResource(
                SettingsR.plurals.settings_edits_track_count,
                dialog.trackCount,
                dialog.trackCount,
            )
            val artists = pluralStringResource(
                SettingsR.plurals.settings_edits_artist_count,
                dialog.artistCount,
                dialog.artistCount,
            )
            // Only the kinds of edits the filter actually shows are named.
            when {
                dialog.artistCount == 0 -> stringResource(SettingsR.string.settings_edits_confirm_filtered_one, tracks)
                dialog.trackCount == 0 -> stringResource(SettingsR.string.settings_edits_confirm_filtered_one, artists)
                else -> stringResource(SettingsR.string.settings_edits_confirm_filtered, tracks, artists)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(SettingsR.string.settings_edits_confirm_title)) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isBusy) {
                Text(stringResource(SettingsR.string.settings_edits_revert))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(SettingsR.string.settings_edits_cancel))
            }
        },
    )
}

private fun Long.formatDate(): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

@get:StringRes
private val MetadataEditFilter.labelRes: Int
    get() = when (this) {
        MetadataEditFilter.All -> SettingsR.string.settings_edits_filter_all
        MetadataEditFilter.Title -> SettingsR.string.settings_edits_field_title
        MetadataEditFilter.Artist -> SettingsR.string.settings_edits_field_artist
        MetadataEditFilter.Album -> SettingsR.string.settings_edits_field_album
        MetadataEditFilter.Artwork -> SettingsR.string.settings_edits_field_artwork
        MetadataEditFilter.Missing -> SettingsR.string.settings_edits_filter_missing
        MetadataEditFilter.Artists -> SettingsR.string.settings_edits_filter_artists
    }

@get:StringRes
private val EditedField.labelRes: Int
    get() = when (this) {
        EditedField.Title -> SettingsR.string.settings_edits_field_title
        EditedField.Artist -> SettingsR.string.settings_edits_field_artist
        EditedField.Album -> SettingsR.string.settings_edits_field_album
        EditedField.Artwork -> SettingsR.string.settings_edits_field_artwork
    }

@get:StringRes
private val EditedField.restoreLabelRes: Int
    get() = when (this) {
        EditedField.Title -> SettingsR.string.settings_edits_restore_title
        EditedField.Artist -> SettingsR.string.settings_edits_restore_artist
        EditedField.Album -> SettingsR.string.settings_edits_restore_album
        EditedField.Artwork -> SettingsR.string.settings_edits_restore_artwork
    }
