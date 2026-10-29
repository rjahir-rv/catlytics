package com.catlytics.feature.settings.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.catlytics.feature.settings.impl.R as SettingsR

@Composable
internal fun ExportSelectionDialog(
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
                            text = statisticsCountText(
                                stringResource(
                                    SettingsR.string.settings_backup_export_statistics_count,
                                    summary.statistics.eventCount,
                                ),
                                summary.statistics.metadataEditCount,
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
internal fun ImportConfirmDialog(
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
                                text = statisticsCountText(
                                    stringResource(
                                        SettingsR.string.settings_backup_restore_statistics_count,
                                        s.eventCount,
                                    ),
                                    s.metadataEditCount,
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
                                text = stringResource(
                                    SettingsR.string.settings_backup_restore_mode_replace_description,
                                ),
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

private fun formatDate(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

@Composable
private fun statisticsCountText(eventsText: String, metadataEditCount: Int): String =
    if (metadataEditCount > 0) {
        stringResource(
            SettingsR.string.settings_backup_statistics_with_metadata,
            eventsText,
            metadataEditCount,
        )
    } else {
        eventsText
    }
