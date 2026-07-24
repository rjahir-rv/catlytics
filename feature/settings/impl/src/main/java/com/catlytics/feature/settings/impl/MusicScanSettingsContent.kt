package com.catlytics.feature.settings.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.feature.settings.impl.components.SettingsDivider
import com.catlytics.feature.settings.impl.components.SettingsRowText
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.feature.settings.impl.components.SettingsValueRow

@Composable
internal fun MusicScanSettingsContent(
    folders: List<LibraryFolder>,
    settings: MusicScanSettings,
    scanStatus: MusicScanStatus,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onFoldersClick: () -> Unit,
    onDurationFilterChange: (MusicScanDurationFilter) -> Unit,
    onSizeFilterChange: (MusicScanSizeFilter) -> Unit,
    onScanMusic: () -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val visibleFolderCount = folders.count(LibraryFolder::isVisible)
    val isScanning = scanStatus == MusicScanStatus.Scanning

    LazyColumn(
        modifier = modifier,
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
                title = "Biblioteca musical",
                iconRes = R.drawable.ic_library,
            ) {
                SettingsValueRow(
                    title = "Carpetas",
                    supportingText = "Elige qué carpetas aparecen en tu biblioteca",
                    value = "$visibleFolderCount/${folders.size}",
                    onClick = onFoldersClick,
                )
            }
        }
        item {
            SettingsSection(
                title = "Filtros de escaneo",
                iconRes = R.drawable.ic_filter,
            ) {
                DurationFilterSelector(
                    selected = settings.durationFilter,
                    onSelected = onDurationFilterChange,
                )
                SettingsDivider()
                SizeFilterSelector(
                    selected = settings.sizeFilter,
                    onSelected = onSizeFilterChange,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = if (hasAudioPermission) onScanMusic else onRequestAudioPermission,
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AnimatedContent(
                        targetState = isScanning,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "scanButtonContent",
                    ) { scanning ->
                        if (scanning) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text("Escaneando…")
                            }
                        } else {
                            Text(
                                if (hasAudioPermission) {
                                    "Escanear ahora"
                                } else {
                                    "Permitir acceso a música"
                                },
                            )
                        }
                    }
                }
                ScanStatusCard(status = scanStatus)
            }
        }
    }
}

@Composable
private fun DurationFilterSelector(
    selected: MusicScanDurationFilter,
    onSelected: (MusicScanDurationFilter) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.animateContentSize()) {
        SettingsValueRow(
            title = "Ignorar duración",
            supportingText = "Excluir canciones más cortas que el límite",
            value = selected.label,
            onClick = { expanded = !expanded },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                SettingsDivider()
                Column(modifier = Modifier.selectableGroup()) {
                    MusicScanDurationFilter.entries.forEach { filter ->
                        ScanFilterOption(
                            title = filter.label,
                            selected = selected == filter,
                            onClick = {
                                onSelected(filter)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeFilterSelector(
    selected: MusicScanSizeFilter,
    onSelected: (MusicScanSizeFilter) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.animateContentSize()) {
        SettingsValueRow(
            title = "Ignorar tamaño",
            supportingText = "Excluir archivos más pequeños que el límite",
            value = selected.label,
            onClick = { expanded = !expanded },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                SettingsDivider()
                Column(modifier = Modifier.selectableGroup()) {
                    MusicScanSizeFilter.entries.forEach { filter ->
                        ScanFilterOption(
                            title = filter.label,
                            selected = selected == filter,
                            onClick = {
                                onSelected(filter)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanFilterOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ScanStatusCard(status: MusicScanStatus) {
    AnimatedVisibility(
        visible = status != MusicScanStatus.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        AnimatedContent(
            targetState = status,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "scanStatus",
        ) { currentStatus ->
            val isError = currentStatus is MusicScanStatus.Error
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                color = if (isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (currentStatus) {
                            MusicScanStatus.Scanning -> CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            is MusicScanStatus.Success -> Icon(
                                painter = painterResource(R.drawable.ic_check_list),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            is MusicScanStatus.Error -> Icon(
                                painter = painterResource(R.drawable.ic_info),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                            MusicScanStatus.Idle -> Unit
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = currentStatus.title,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = currentStatus.supportingText,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (currentStatus == MusicScanStatus.Scanning) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ScanFoldersContent(
    folders: List<LibraryFolder>,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val sortedFolders = remember(folders) { folders.sortedBy { it.path.lowercase() } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = scaffoldContentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = bottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Carpetas musicales",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Los cambios también se reflejan en la sección Carpetas de Library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (sortedFolders.isEmpty()) {
            item {
                Text(
                    text = "Escanea el dispositivo para encontrar carpetas con música.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(sortedFolders, key = LibraryFolder::id) { folder ->
            FolderSelectionRow(
                folder = folder,
                onVisibilityChange = { visible ->
                    onFolderVisibilityChange(folder.id, visible)
                },
            )
        }
    }
}

@Composable
private fun FolderSelectionRow(
    folder: LibraryFolder,
    onVisibilityChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = folder.isVisible,
                onValueChange = onVisibilityChange,
                role = Role.Checkbox,
            )
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = folder.isVisible, onCheckedChange = null)
        SettingsRowText(
            title = folder.name,
            supportingText = "${folder.path} · ${folder.trackCount} canciones",
            modifier = Modifier.weight(1f),
        )
    }
}

private val MusicScanDurationFilter.label: String
    get() = when (this) {
        MusicScanDurationFilter.Disabled -> "No ignorar"
        MusicScanDurationFilter.Seconds30 -> "30 segundos"
        MusicScanDurationFilter.Seconds60 -> "60 segundos"
    }

private val MusicScanSizeFilter.label: String
    get() = when (this) {
        MusicScanSizeFilter.Disabled -> "No ignorar"
        MusicScanSizeFilter.Kilobytes500 -> "500 KB"
        MusicScanSizeFilter.Megabyte1 -> "1 MB"
    }

private val MusicScanStatus.title: String
    get() = when (this) {
        MusicScanStatus.Idle -> ""
        MusicScanStatus.Scanning -> "Buscando música"
        is MusicScanStatus.Success -> "Escaneo completado"
        is MusicScanStatus.Error -> "No se pudo completar"
    }

private val MusicScanStatus.supportingText: String
    get() = when (this) {
        MusicScanStatus.Idle -> ""
        MusicScanStatus.Scanning -> "Revisando carpetas y aplicando tus filtros…"
        is MusicScanStatus.Success -> when (newTrackCount) {
            0 -> "No se encontraron canciones nuevas."
            1 -> "Se agregó 1 canción nueva."
            else -> "Se agregaron $newTrackCount canciones nuevas."
        }
        is MusicScanStatus.Error -> message
    }
