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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.feature.settings.impl.components.SettingsDivider
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.feature.settings.impl.components.SettingsValueRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                title = "Copia de seguridad",
                iconRes = R.drawable.ic_line_chart,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Exporta un archivo de respaldo para proteger tus listas de reproducción y tu historial de escucha en caso de desinstalar la app o migrar de teléfono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "🎵 Playlists: ${summary.playlists.playlistCount} listas (${summary.playlists.totalTracks} canciones, ${summary.playlists.likedTracksCount} favoritas)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "📊 Estadísticas: ${summary.statistics.eventCount} reproducciones registradas" +
                            if (summary.statistics.artistAliasCount > 0) ", ${summary.statistics.artistAliasCount} artistas unificados" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                SettingsDivider()
                SettingsValueRow(
                    title = "Exportar copia de seguridad",
                    supportingText = "Elige si deseas exportar playlists, estadísticas o ambos en un archivo JSON",
                    showChevron = !isBusy,
                    onClick = {
                        if (!isBusy) showExportSelectionDialog = true
                    },
                )
                SettingsDivider()
                SettingsValueRow(
                    title = "Restaurar copia de seguridad",
                    supportingText = "Carga tus datos desde un archivo JSON previamente exportado",
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
                                UnifiedBackupStatus.Exporting -> "Generando copia de seguridad…"
                                UnifiedBackupStatus.Importing -> "Restaurando datos…"
                                UnifiedBackupStatus.LoadingPreview -> "Leyendo archivo…"
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
                val stats = operationStatus.result.statistics
                val playlists = operationStatus.result.playlists
                val parts = mutableListOf<String>()
                if (playlists != null) parts.add("${playlists.importedPlaylistsCount} playlists")
                if (stats != null) parts.add("${stats.importedCount} reproducciones")

                item {
                    StatusMessage(
                        text = "Restauración completada con éxito: ${parts.joinToString(", ")}.",
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
                        text = operationStatus.message,
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
            Text("Exportar copia de seguridad", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Selecciona qué datos deseas incluir en el archivo de respaldo:",
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
                            text = "Listas de reproducción (Playlists)",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${summary.playlists.playlistCount} listas (${summary.playlists.totalTracks} canciones)",
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
                            text = "Estadísticas de escucha",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${summary.statistics.eventCount} reproducciones registradas",
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
                Text("Exportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
            Text("Restaurar copia de seguridad", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Archivo exportado el ${formatDate(preview.exportedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (hasPlaylists || hasStats) {
                    Text(
                        text = "Contenido a restaurar:",
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
                                text = "Listas de reproducción",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val p = preview.playlists!!
                            Text(
                                text = "${p.playlistCount} playlists (${p.totalTracksInBackup} pistas)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (p.missingTracksCount > 0) {
                                Text(
                                    text = "Aviso: ${p.missingTracksCount} canciones no están en este dispositivo.",
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
                                text = "Estadísticas de escucha",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val s = preview.statistics!!
                            Text(
                                text = "${s.eventCount} eventos de escucha",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    text = "Modo de restauración:",
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
                                text = "Combinar con los datos actuales",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Conserva lo que tienes en la app y añade o actualiza los datos del archivo.",
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
                                text = "Reemplazar datos actuales",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Elimina los datos actuales en la app y los sustituye por los del archivo.",
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
                Text("Restaurar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
                text = "⚠️ Canciones no encontradas en el dispositivo ($missingCount)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Las playlists se importaron omitiendo las pistas que no se encontraron en tu almacenamiento local:",
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
                            text = "• $name",
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
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
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
                    text = "Cerrar",
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

