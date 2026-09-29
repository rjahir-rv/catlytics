package com.catlytics.feature.settings.impl

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.feature.settings.impl.R as SettingsR
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
                title = stringResource(SettingsR.string.settings_scan_library_section),
                iconRes = R.drawable.ic_library,
            ) {
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_folders_title),
                    supportingText = stringResource(SettingsR.string.settings_folders_supporting),
                    value = "$visibleFolderCount/${folders.size}",
                    onClick = onFoldersClick,
                )
            }
        }
        item {
            SettingsSection(
                title = stringResource(SettingsR.string.settings_scan_filters_section),
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
                                Text(stringResource(SettingsR.string.settings_scan_button_scanning))
                            }
                        } else {
                            Text(
                                if (hasAudioPermission) {
                                    stringResource(SettingsR.string.settings_scan_button_scan)
                                } else {
                                    stringResource(SettingsR.string.settings_scan_button_permission)
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
            title = stringResource(SettingsR.string.settings_scan_duration_title),
            supportingText = stringResource(SettingsR.string.settings_scan_duration_supporting),
            value = stringResource(selected.labelRes),
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
                            title = stringResource(filter.labelRes),
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
            title = stringResource(SettingsR.string.settings_scan_size_title),
            supportingText = stringResource(SettingsR.string.settings_scan_size_supporting),
            value = stringResource(selected.labelRes),
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
                            title = stringResource(filter.labelRes),
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
                                text = currentStatus.title(),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = currentStatus.supportingText(),
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
        modifier = modifier.fillMaxSize(),
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
                    text = stringResource(SettingsR.string.settings_folders_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(SettingsR.string.settings_folders_screen_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (sortedFolders.isEmpty()) {
            item {
                Text(
                    text = stringResource(SettingsR.string.settings_folders_empty),
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
            supportingText = stringResource(
                SettingsR.string.settings_folder_supporting,
                folder.path,
                folder.trackCount,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@get:StringRes
private val MusicScanDurationFilter.labelRes: Int
    get() = when (this) {
        MusicScanDurationFilter.Disabled -> SettingsR.string.settings_scan_filter_disabled
        MusicScanDurationFilter.Seconds30 -> SettingsR.string.settings_scan_duration_30_seconds
        MusicScanDurationFilter.Seconds60 -> SettingsR.string.settings_scan_duration_60_seconds
    }

@get:StringRes
private val MusicScanSizeFilter.labelRes: Int
    get() = when (this) {
        MusicScanSizeFilter.Disabled -> SettingsR.string.settings_scan_filter_disabled
        MusicScanSizeFilter.Kilobytes500 -> SettingsR.string.settings_scan_size_500_kb
        MusicScanSizeFilter.Megabyte1 -> SettingsR.string.settings_scan_size_1_mb
    }

@Composable
private fun MusicScanStatus.title(): String = when (this) {
    MusicScanStatus.Idle -> ""
    MusicScanStatus.Scanning -> stringResource(SettingsR.string.settings_scan_status_searching)
    is MusicScanStatus.Success -> stringResource(SettingsR.string.settings_scan_status_success)
    is MusicScanStatus.Error -> stringResource(SettingsR.string.settings_scan_status_error)
}

@Composable
private fun MusicScanStatus.supportingText(): String = when (this) {
    MusicScanStatus.Idle -> ""
    MusicScanStatus.Scanning ->
        stringResource(SettingsR.string.settings_scan_status_scanning_supporting)
    is MusicScanStatus.Success -> when (newTrackCount) {
        0 -> stringResource(SettingsR.string.settings_scan_success_no_new_tracks)
        else -> pluralStringResource(
            SettingsR.plurals.settings_scan_success_new_tracks,
            newTrackCount,
            newTrackCount,
        )
    }
    is MusicScanStatus.Error -> message.asString()
}
