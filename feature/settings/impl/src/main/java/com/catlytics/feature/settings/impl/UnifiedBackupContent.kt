package com.catlytics.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.feature.settings.impl.R as SettingsR
import com.catlytics.feature.settings.impl.components.SettingsDivider
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.feature.settings.impl.components.SettingsValueRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
) {
    val isBusy = operationStatus is UnifiedBackupStatus.Exporting ||
        operationStatus is UnifiedBackupStatus.Importing ||
        operationStatus is UnifiedBackupStatus.LoadingPreview

    var showExportSelectionDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = scaffoldContentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = bottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
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
            is UnifiedBackupStatus.ExportSuccess -> {
                // Notificado mediante Toast
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
private fun ExportSelectionDialog(
    summary: UnifiedBackupSummary,
    onConfirm: (BackupOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var includePlaylists by rememberSaveable { mutableStateOf(true) }
    var includeStats by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(SettingsR.string.settings_backup_export_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(SettingsR.string.settings_backup_export_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { includePlaylists = !includePlaylists }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = includePlaylists,
                        onCheckedChange = { includePlaylists = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(SettingsR.string.settings_backup_export_playlists_option),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(
                                SettingsR.string.settings_backup_export_playlists_count,
                                summary.playlists.playlistCount,
                                summary.playlists.totalTracks,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { includeStats = !includeStats }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = includeStats,
                        onCheckedChange = { includeStats = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(SettingsR.string.settings_backup_export_statistics_option),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(
                                SettingsR.string.settings_backup_export_statistics_count,
                                summary.statistics.eventCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = includePlaylists || includeStats,
                onClick = {
                    onConfirm(
                        BackupOptions(
                            includeStatistics = includeStats,
                            includePlaylists = includePlaylists,
                        ),
                    )
                },
            ) {
                Text(stringResource(SettingsR.string.settings_action_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(SettingsR.string.settings_action_cancel))
            }
        },
    )
}

@Composable
private fun ImportConfirmDialog(
    preview: UnifiedBackupPreview,
    onConfirm: (BackupOptions, StatisticsImportMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val hasStats = preview.statistics != null
    val hasPlaylists = preview.playlists != null
    var restoreStats by rememberSaveable { mutableStateOf(hasStats) }
    var restorePlaylists by rememberSaveable { mutableStateOf(hasPlaylists) }
    var selectedMode by rememberSaveable { mutableStateOf(StatisticsImportMode.Merge) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(SettingsR.string.settings_backup_restore_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(
                        SettingsR.string.settings_backup_exported_at,
                        formatDate(preview.exportedAtMillis),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (hasPlaylists || hasStats) {
                    Text(
                        text = stringResource(SettingsR.string.settings_backup_restore_content_title),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                if (hasPlaylists) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = hasStats) { restorePlaylists = !restorePlaylists }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hasStats) {
                            Checkbox(
                                checked = restorePlaylists,
                                onCheckedChange = { restorePlaylists = it },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                text = stringResource(SettingsR.string.settings_backup_restore_playlists_option),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val p = preview.playlists!!
                            Text(
                                text = stringResource(
                                    SettingsR.string.settings_backup_restore_playlists_count,
                                    p.playlistCount,
                                    p.totalTracksInBackup,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (p.missingTracksCount > 0) {
                                Text(
                                    text = stringResource(
                                        SettingsR.string.settings_backup_restore_missing_warning,
                                        p.missingTracksCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                if (hasStats) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = hasPlaylists) { restoreStats = !restoreStats }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hasPlaylists) {
                            Checkbox(
                                checked = restoreStats,
                                onCheckedChange = { restoreStats = it },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                text = stringResource(SettingsR.string.settings_backup_export_statistics_option),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val s = preview.statistics!!
                            Text(
                                text = stringResource(
                                    SettingsR.string.settings_backup_restore_statistics_count,
                                    s.eventCount,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(SettingsR.string.settings_backup_restore_mode_title),
                    style = MaterialTheme.typography.labelLarge,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedMode == StatisticsImportMode.Merge,
                                onClick = { selectedMode = StatisticsImportMode.Merge },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedMode == StatisticsImportMode.Merge,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(SettingsR.string.settings_backup_restore_mode_merge_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(SettingsR.string.settings_backup_restore_mode_merge_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedMode == StatisticsImportMode.Replace,
                                onClick = { selectedMode = StatisticsImportMode.Replace },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedMode == StatisticsImportMode.Replace,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(SettingsR.string.settings_backup_restore_mode_replace_title),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(SettingsR.string.settings_backup_restore_mode_replace_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = restorePlaylists || restoreStats,
                onClick = {
                    onConfirm(
                        BackupOptions(
                            includeStatistics = restoreStats,
                            includePlaylists = restorePlaylists,
                        ),
                        selectedMode,
                    )
                },
            ) {
                Text(stringResource(SettingsR.string.settings_action_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(SettingsR.string.settings_action_cancel))
            }
        },
    )
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

private fun formatDate(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
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

