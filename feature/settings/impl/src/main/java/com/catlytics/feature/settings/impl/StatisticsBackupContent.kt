package com.catlytics.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.StatisticsBackupPreview
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.feature.settings.impl.components.SettingsDivider
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.feature.settings.impl.components.SettingsValueRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun StatisticsBackupContent(
    summary: StatisticsBackupSummary,
    operationStatus: StatisticsBackupStatus,
    importPreview: StatisticsBackupPreview?,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImport: (StatisticsImportMode) -> Unit,
    onDismissImportPreview: () -> Unit,
    onDismissStatus: () -> Unit,
    bottomPadding: () -> Dp,
    modifier: Modifier = Modifier,
) {
    val isBusy = operationStatus is StatisticsBackupStatus.Exporting ||
        operationStatus is StatisticsBackupStatus.Importing ||
        operationStatus is StatisticsBackupStatus.LoadingPreview

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 24.dp,
            end = 20.dp,
            bottom = bottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            SettingsSection(
                title = "Historial de escucha",
                iconRes = R.drawable.ic_line_chart,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Guarda un archivo para recuperar tus estadísticas si reinstalas " +
                            "la app o borras los datos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatSummary(summary),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                SettingsDivider()
                SettingsValueRow(
                    title = "Exportar respaldo",
                    supportingText = "Crea un archivo JSON con tus eventos de escucha",
                    showChevron = !isBusy,
                    onClick = {
                        if (!isBusy) onExportClick()
                    },
                )
                SettingsDivider()
                SettingsValueRow(
                    title = "Importar respaldo",
                    supportingText = "Restaura estadísticas desde un archivo guardado",
                    showChevron = !isBusy,
                    onClick = {
                        if (!isBusy) onImportClick()
                    },
                )
            }
        }

        when (operationStatus) {
            is StatisticsBackupStatus.Exporting,
            is StatisticsBackupStatus.Importing,
            is StatisticsBackupStatus.LoadingPreview,
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
                                StatisticsBackupStatus.Exporting -> "Exportando respaldo…"
                                StatisticsBackupStatus.Importing -> "Importando respaldo…"
                                StatisticsBackupStatus.LoadingPreview -> "Leyendo archivo…"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            is StatisticsBackupStatus.ExportSuccess -> {
                item {
                    StatusMessage(
                        text = buildString {
                            append("Exportado: ${operationStatus.eventCount} eventos")
                            if (operationStatus.artistAliasCount > 0) {
                                append(" · ${operationStatus.artistAliasCount} fusiones")
                            }
                            append('.')
                        },
                        isError = false,
                        onDismiss = onDismissStatus,
                    )
                }
            }
            is StatisticsBackupStatus.ImportSuccess -> {
                item {
                    StatusMessage(
                        text = buildString {
                            append("Importados ${operationStatus.importedCount}")
                            if (operationStatus.skippedDuplicateCount > 0) {
                                append(" · omitidos ${operationStatus.skippedDuplicateCount} duplicados")
                            }
                            append(" (archivo: ${operationStatus.totalInFile}).")
                            if (operationStatus.importedArtistAliasCount > 0) {
                                append(" Fusiones importadas: ")
                                append(operationStatus.importedArtistAliasCount)
                                append('.')
                            }
                        },
                        isError = false,
                        onDismiss = onDismissStatus,
                    )
                }
            }
            is StatisticsBackupStatus.Error -> {
                item {
                    StatusMessage(
                        text = operationStatus.message,
                        isError = true,
                        onDismiss = onDismissStatus,
                    )
                }
            }
            StatisticsBackupStatus.Idle -> Unit
        }
    }

    if (importPreview != null) {
        ImportConfirmDialog(
            preview = importPreview,
            onMerge = { onConfirmImport(StatisticsImportMode.Merge) },
            onReplace = { onConfirmImport(StatisticsImportMode.Replace) },
            onDismiss = onDismissImportPreview,
        )
    }
}

@Composable
private fun StatusMessage(
    text: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        TextButton(onClick = onDismiss) {
            Text("Cerrar")
        }
    }
}

@Composable
private fun ImportConfirmDialog(
    preview: StatisticsBackupPreview,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar respaldo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Se encontraron ${preview.eventCount} eventos " +
                        "(esquema v${preview.schemaVersion}).",
                )
                if (preview.artistAliasCount > 0) {
                    Text("Incluye ${preview.artistAliasCount} fusiones de artistas.")
                }
                Text(
                    text = "Exportado: ${formatDateTime(preview.exportedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val firstEvent = preview.firstEventMillis
                val lastEvent = preview.lastEventMillis
                if (firstEvent != null && lastEvent != null) {
                    Text(
                        text = "Rango: ${formatDate(firstEvent)} – ${formatDate(lastEvent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Fusionar añade solo eventos nuevos. " +
                        "Reemplazar sustituye las estadísticas y, en respaldos v2, " +
                        "las fusiones de artistas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (preview.eventCount == 0) {
                    Text(
                        text = "Este respaldo está vacío. Reemplazar eliminará todo el historial local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onMerge) {
                Text("Fusionar")
            }
        },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onReplace) {
                    Text("Reemplazar todo")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        },
    )
}

private fun formatSummary(summary: StatisticsBackupSummary): String {
    if (summary.eventCount <= 0L) {
        return if (summary.artistAliasCount > 0) {
            "Sin eventos · ${summary.artistAliasCount} fusiones"
        } else {
            "Sin datos todavía"
        }
    }
    val countLabel = if (summary.eventCount == 1L) {
        "1 evento"
    } else {
        "${summary.eventCount} eventos"
    }
    val first = summary.firstEventMillis?.let(::formatDate)
    val last = summary.lastEventMillis?.let(::formatDate)
    val events = when {
        first != null && last != null && first != last -> "$countLabel · $first – $last"
        first != null -> "$countLabel · desde $first"
        else -> countLabel
    }
    return if (summary.artistAliasCount > 0) {
        "$events · ${summary.artistAliasCount} fusiones"
    } else {
        events
    }
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es"))

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es"))

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(dateFormatter)

private fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(dateTimeFormatter)
