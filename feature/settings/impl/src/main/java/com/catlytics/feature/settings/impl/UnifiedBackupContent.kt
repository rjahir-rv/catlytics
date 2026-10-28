package com.catlytics.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import com.catlytics.core.designsystem.layout.deferredPadding
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.feature.settings.impl.components.SettingsDivider
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.feature.settings.impl.components.SettingsValueRow
import com.catlytics.feature.settings.impl.R as SettingsR

@Composable
internal fun UnifiedBackupContent(
    summary: UnifiedBackupSummary,
    operationStatus: UnifiedBackupStatus,
    importPreview: UnifiedBackupPreview?,
    onExportClick: (BackupOptions) -> Unit,
    onImportClick: () -> Unit,
    onConfirmImport: (BackupOptions, StatisticsImportMode) -> Unit,
    onDismissImportPreview: () -> Unit,
    onDismissStatus: () -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onViewMetadataEdits: () -> Unit = {},
) {
    val isBusy = operationStatus is UnifiedBackupStatus.Exporting ||
        operationStatus is UnifiedBackupStatus.Importing ||
        operationStatus is UnifiedBackupStatus.LoadingPreview

    var showExportSelectionDialog by rememberSaveable { mutableStateOf(false) }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(CatlyticsLayoutTokens.CardMinColumnWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = deferredPadding(
            start = 20.dp,
            top = { scaffoldContentPadding.calculateTopPadding() + 24.dp },
            end = 20.dp,
            bottom = { bottomPadding() + 24.dp },
        ),
        verticalItemSpacing = 20.dp,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            SettingsSection(
                title = stringResource(SettingsR.string.settings_backup_title),
                iconRes = R.drawable.ic_line_chart,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(SettingsR.string.settings_backup_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            SettingsR.string.settings_backup_playlists_summary,
                            summary.playlists.playlistCount,
                            summary.playlists.totalTracks,
                            summary.playlists.likedTracksCount,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (summary.statistics.artistAliasCount > 0) {
                            stringResource(
                                SettingsR.string.settings_backup_statistics_summary_artists,
                                summary.statistics.eventCount,
                                summary.statistics.artistAliasCount,
                            )
                        } else {
                            stringResource(
                                SettingsR.string.settings_backup_statistics_summary,
                                summary.statistics.eventCount,
                            )
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (summary.statistics.metadataEditCount > 0) {
                        Text(
                            text = stringResource(
                                SettingsR.string.settings_backup_metadata_summary,
                                summary.statistics.metadataEditCount,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (summary.statistics.metadataEditCount > 0 || summary.statistics.artistAliasCount > 0) {
                        TextButton(
                            onClick = onViewMetadataEdits,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(stringResource(SettingsR.string.settings_backup_view_edits))
                        }
                    }
                }
                SettingsDivider()
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_backup_export_title),
                    supportingText = stringResource(SettingsR.string.settings_backup_export_supporting),
                    showChevron = !isBusy,
                    onClick = {
                        if (!isBusy) showExportSelectionDialog = true
                    },
                )
                SettingsDivider()
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_backup_restore_title),
                    supportingText = stringResource(SettingsR.string.settings_backup_restore_supporting),
                    showChevron = !isBusy,
                    onClick = {
                        if (!isBusy) onImportClick()
                    },
                )
            }
        }

        when (operationStatus) {
            is UnifiedBackupStatus.Exporting,
            is UnifiedBackupStatus.Importing,
            is UnifiedBackupStatus.LoadingPreview,
            -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = when (operationStatus) {
                                UnifiedBackupStatus.Exporting ->
                                    stringResource(SettingsR.string.settings_backup_exporting)
                                UnifiedBackupStatus.Importing ->
                                    stringResource(SettingsR.string.settings_backup_importing)
                                UnifiedBackupStatus.LoadingPreview ->
                                    stringResource(SettingsR.string.settings_backup_reading_file)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            is UnifiedBackupStatus.ImportSuccess -> {
                val playlists = operationStatus.result.playlists
                item {
                    val stats = operationStatus.result.statistics
                    val parts = buildList {
                        if (playlists != null) {
                            add(
                                stringResource(
                                    SettingsR.string.settings_backup_import_part_playlists,
                                    playlists.importedPlaylistsCount,
                                ),
                            )
                        }
                        if (stats != null && stats.importedMetadataEditCount > 0) {
                            add(
                                stringResource(
                                    SettingsR.string.settings_backup_import_part_metadata,
                                    stats.importedMetadataEditCount,
                                ),
                            )
                        }
                        if (stats != null) {
                            add(
                                stringResource(
                                    SettingsR.string.settings_backup_import_part_listens,
                                    stats.importedCount,
                                ),
                            )
                        }
                    }

                    StatusMessage(
                        text = stringResource(
                            SettingsR.string.settings_backup_import_success,
                            parts.joinToString(", "),
                        ),
                        isError = false,
                        onDismiss = onDismissStatus,
                    )
                }

                // Opción B: Si hubo canciones no encontradas en playlists, mostrar alerta destacada
                if (playlists != null && playlists.missingTracksCount > 0) {
                    item {
                        MissingTracksWarningCard(
                            missingCount = playlists.missingTracksCount,
                            missingTrackNames = playlists.missingTrackNames,
                        )
                    }
                }
            }
            is UnifiedBackupStatus.Error -> {
                item {
                    StatusMessage(
                        text = operationStatus.message.asString(),
                        isError = true,
                        onDismiss = onDismissStatus,
                    )
                }
            }
            UnifiedBackupStatus.Idle -> Unit
        }
    }

    if (showExportSelectionDialog) {
        ExportSelectionDialog(
            summary = summary,
            onConfirm = { options ->
                showExportSelectionDialog = false
                onExportClick(options)
            },
            onDismiss = { showExportSelectionDialog = false },
        )
    }

    importPreview?.let { preview ->
        ImportConfirmDialog(
            preview = preview,
            onConfirm = onConfirmImport,
            onDismiss = onDismissImportPreview,
        )
    }
}

@Composable
private fun MissingTracksWarningCard(
    missingCount: Int,
    missingTrackNames: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    SettingsR.string.settings_backup_missing_tracks_title,
                    missingCount,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(SettingsR.string.settings_backup_missing_tracks_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (missingTrackNames.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(missingTrackNames) { name ->
                        Text(
                            text = stringResource(
                                SettingsR.string.settings_backup_missing_track_name,
                                name,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(
    text: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(SettingsR.string.settings_action_close),
                    color = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                )
            }
        }
    }
}
