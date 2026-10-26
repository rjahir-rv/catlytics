package com.catlytics.feature.library.impl.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.model.Artist
import com.catlytics.core.designsystem.component.CollapsibleHint
import com.catlytics.core.designsystem.component.InfoToggleButton
import com.catlytics.core.model.ArtistAlias
import com.catlytics.feature.library.impl.R as LibraryR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArtistMergeDialogs(
    state: LibraryArtistUiState.Success,
    onQueryChange: (String) -> Unit,
    onTargetSelected: (Artist) -> Unit,
    onUnmergeRequested: (ArtistAlias) -> Unit,
    onDismiss: () -> Unit,
    onConfirmMerge: () -> Unit,
    onConfirmUnmerge: () -> Unit,
    onRenameNameChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
) {
    when (val dialog = state.mergeDialog) {
        ArtistMergeDialog.Hidden -> Unit
        is ArtistMergeDialog.SelectTarget -> {
            val candidates = remember(state.mergeCandidates, dialog.query) {
                val query = dialog.query.trim()
                if (query.isEmpty()) state.mergeCandidates else state.mergeCandidates.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
            ModalBottomSheet(onDismissRequest = onDismiss) {
                Text(
                    text = stringResource(LibraryR.string.library_artist_merge_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Text(
                    text = stringResource(LibraryR.string.library_artist_merge_picker_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                OutlinedTextField(
                    value = dialog.query,
                    onValueChange = onQueryChange,
                    label = {
                        Text(stringResource(LibraryR.string.library_artist_merge_search_hint))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(candidates, key = Artist::id) { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name.displayArtistName()) },
                            modifier = Modifier.clickable { onTargetSelected(artist) },
                        )
                    }
                }
            }
        }
        is ArtistMergeDialog.ConfirmMerge -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(LibraryR.string.library_artist_merge_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        LibraryR.string.library_artist_merge_confirm_message,
                        state.content.summary.artist.name.displayArtistName(),
                        dialog.target.name.displayArtistName(),
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmMerge, enabled = !state.isMergeBusy) {
                    Text(
                        if (state.isMergeBusy) {
                            stringResource(LibraryR.string.library_action_merging)
                        } else {
                            stringResource(LibraryR.string.library_action_merge)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !state.isMergeBusy) {
                    Text(stringResource(LibraryR.string.library_action_cancel))
                }
            },
        )
        ArtistMergeDialog.ManageAliases -> ModalBottomSheet(onDismissRequest = onDismiss) {
            Text(
                text = stringResource(LibraryR.string.library_artist_aliases_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            state.aliases.forEachIndexed { index, alias ->
                ListItem(
                    headlineContent = { Text(alias.source.name) },
                    supportingContent = {
                        Text(
                            if (alias.isDisplayRename) {
                                stringResource(LibraryR.string.library_artist_alias_original_name)
                            } else {
                                stringResource(LibraryR.string.library_artist_alias_target, alias.target.name)
                            },
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { onUnmergeRequested(alias) }) {
                            Text(
                                stringResource(
                                    if (alias.isDisplayRename) {
                                        LibraryR.string.library_action_restore_name
                                    } else {
                                        LibraryR.string.library_action_unmerge
                                    },
                                ),
                            )
                        }
                    },
                )
                if (index < state.aliases.lastIndex) HorizontalDivider()
            }
        }
        is ArtistMergeDialog.Rename -> {
            var showDetails by rememberSaveable { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(LibraryR.string.library_artist_rename_title),
                            modifier = Modifier.weight(1f),
                        )
                        InfoToggleButton(expanded = showDetails, onExpandedChange = { showDetails = it })
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(LibraryR.string.library_artist_rename_description))
                        CollapsibleHint(
                            visible = showDetails,
                            text = stringResource(LibraryR.string.library_artist_rename_details),
                        )
                        OutlinedTextField(
                            value = dialog.name,
                            onValueChange = onRenameNameChange,
                            label = { Text(stringResource(LibraryR.string.library_artist_rename_field)) },
                            singleLine = true,
                            supportingText = state.renameMergeTarget?.let { target ->
                                { Text(stringResource(LibraryR.string.library_artist_rename_existing, target.name)) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmRename,
                        enabled = dialog.name.isNotBlank() && !state.isMergeBusy,
                    ) {
                        Text(
                            if (state.isMergeBusy) {
                                stringResource(LibraryR.string.library_action_renaming)
                            } else {
                                stringResource(LibraryR.string.library_action_rename)
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss, enabled = !state.isMergeBusy) {
                        Text(stringResource(LibraryR.string.library_action_cancel))
                    }
                },
            )
        }
        is ArtistMergeDialog.ConfirmUnmerge -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(LibraryR.string.library_artist_unmerge_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        LibraryR.string.library_artist_unmerge_confirm_message,
                        dialog.alias.source.name,
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmUnmerge, enabled = !state.isMergeBusy) {
                    Text(
                        if (state.isMergeBusy) {
                            stringResource(LibraryR.string.library_action_unmerging)
                        } else {
                            stringResource(LibraryR.string.library_action_unmerge)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !state.isMergeBusy) {
                    Text(stringResource(LibraryR.string.library_action_cancel))
                }
            },
        )
    }
}
