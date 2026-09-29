package com.catlytics.feature.settings.impl

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.MAX_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.MIN_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
import com.catlytics.core.model.HomeRecommendationsSettings
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.ThemeMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.feature.settings.impl.R as SettingsR
import com.catlytics.feature.settings.impl.components.SettingsDivider
import com.catlytics.feature.settings.impl.components.SettingsRowText
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.feature.settings.impl.components.SettingsToggleRow
import com.catlytics.feature.settings.impl.components.SettingsValueRow
import com.catlytics.feature.settings.impl.equalizer.EqualizerSettingsContent

@Composable
internal fun SettingsScreen(
    appVersion: String,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    homeRecommendationsSettings: HomeRecommendationsSettings,
    equalizerState: EqualizerState,
    crossfadeDurationSeconds: Int,
    sleepTimerState: SleepTimerState,
    libraryFolders: List<LibraryFolder>,
    musicScanSettings: MusicScanSettings,
    musicScanStatus: MusicScanStatus,
    hasAudioPermission: Boolean,
    unifiedBackupSummary: UnifiedBackupSummary = UnifiedBackupSummary(),
    unifiedBackupStatus: UnifiedBackupStatus = UnifiedBackupStatus.Idle,
    unifiedImportPreview: UnifiedBackupPreview? = null,
    onRequestAudioPermission: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onShowRecommendedPlaylistsChange: (Boolean) -> Unit,
    onRecentAddedWindowChange: (RecentAddedWindow) -> Unit,
    onShowNewTrackBadgeChange: (Boolean) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    onSleepTimerStart: (Int) -> Unit,
    onSleepTimerCancel: () -> Unit,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    onMusicScanDurationFilterChange: (MusicScanDurationFilter) -> Unit,
    onMusicScanSizeFilterChange: (MusicScanSizeFilter) -> Unit,
    onScanMusic: () -> Unit,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    onEqualizerModeChange: (EqualizerMode) -> Unit,
    onEqualizerPresetSelected: (EqualizerPreset) -> Unit,
    onCustomBandLevelChange: (Short, Int, Boolean) -> Unit,
    onExportBackupClick: (BackupOptions) -> Unit = {},
    onImportBackupClick: () -> Unit = {},
    onConfirmImport: (BackupOptions, StatisticsImportMode) -> Unit = { _, _ -> },
    onDismissImportPreview: () -> Unit = {},
    onDismissBackupStatus: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {},
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.Main) }
    var showSleepTimerSheet by rememberSaveable { mutableStateOf(false) }

    fun navigateBack() {
        destination = when (destination) {
            SettingsDestination.ScanFolders -> SettingsDestination.MusicScan
            SettingsDestination.About,
            SettingsDestination.Equalizer,
            SettingsDestination.MusicScan,
            SettingsDestination.Backup -> SettingsDestination.Main
            SettingsDestination.Main -> SettingsDestination.Main
        }
    }

    BackHandler(enabled = destination != SettingsDestination.Main, onBack = ::navigateBack)

    val mainTitle = stringResource(SettingsR.string.settings_title)
    val equalizerTitle = stringResource(SettingsR.string.settings_equalizer_title)
    val aboutTitle = stringResource(SettingsR.string.settings_about_title)
    val musicScanTitle = stringResource(SettingsR.string.settings_music_scan_title)
    val foldersTitle = stringResource(SettingsR.string.settings_folders_title)
    val backupTitle = stringResource(SettingsR.string.settings_backup_title)

    LaunchedEffect(destination) {
        when (destination) {
            SettingsDestination.Main -> {
                onTopBarTitleChange(mainTitle)
                onTopBarBackActionChange(null)
            }
            SettingsDestination.Equalizer -> {
                onTopBarTitleChange(equalizerTitle)
                onTopBarBackActionChange(::navigateBack)
            }
            SettingsDestination.About -> {
                onTopBarTitleChange(aboutTitle)
                onTopBarBackActionChange(::navigateBack)
            }
            SettingsDestination.MusicScan -> {
                onTopBarTitleChange(musicScanTitle)
                onTopBarBackActionChange(::navigateBack)
            }
            SettingsDestination.ScanFolders -> {
                onTopBarTitleChange(foldersTitle)
                onTopBarBackActionChange(::navigateBack)
            }
            SettingsDestination.Backup -> {
                onTopBarTitleChange(backupTitle)
                onTopBarBackActionChange(::navigateBack)
            }
        }
    }

    when (destination) {
        SettingsDestination.Main -> SettingsMainContent(
            appVersion = appVersion,
            themeMode = themeMode,
            homeRecommendationsSettings = homeRecommendationsSettings,
            equalizerState = equalizerState,
            crossfadeDurationSeconds = crossfadeDurationSeconds,
            sleepTimerState = sleepTimerState,
            onThemeModeChange = onThemeModeChange,
            onShowRecommendedPlaylistsChange = onShowRecommendedPlaylistsChange,
            onRecentAddedWindowChange = onRecentAddedWindowChange,
            onShowNewTrackBadgeChange = onShowNewTrackBadgeChange,
            onCrossfadeDurationChange = onCrossfadeDurationChange,
            onSleepTimerClick = { showSleepTimerSheet = true },
            onEqualizerClick = { destination = SettingsDestination.Equalizer },
            onMusicScanClick = { destination = SettingsDestination.MusicScan },
            onBackupClick = { destination = SettingsDestination.Backup },
            onAboutClick = { destination = SettingsDestination.About },
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
        )
        SettingsDestination.About -> AboutSettingsContent(
            appVersion = appVersion,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
        )
        SettingsDestination.Equalizer -> EqualizerSettingsContent(
            equalizerState = equalizerState,
            onEqualizerEnabledChange = onEqualizerEnabledChange,
            onEqualizerModeChange = onEqualizerModeChange,
            onEqualizerPresetSelected = onEqualizerPresetSelected,
            onCustomBandLevelChange = onCustomBandLevelChange,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
        )
        SettingsDestination.MusicScan -> MusicScanSettingsContent(
            folders = libraryFolders,
            settings = musicScanSettings,
            scanStatus = musicScanStatus,
            hasAudioPermission = hasAudioPermission,
            onRequestAudioPermission = onRequestAudioPermission,
            onFoldersClick = { destination = SettingsDestination.ScanFolders },
            onDurationFilterChange = onMusicScanDurationFilterChange,
            onSizeFilterChange = onMusicScanSizeFilterChange,
            onScanMusic = onScanMusic,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
        )
        SettingsDestination.ScanFolders -> ScanFoldersContent(
            folders = libraryFolders,
            onFolderVisibilityChange = onFolderVisibilityChange,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
        )
        SettingsDestination.Backup -> UnifiedBackupContent(
            summary = unifiedBackupSummary,
            operationStatus = unifiedBackupStatus,
            importPreview = unifiedImportPreview,
            onExportClick = onExportBackupClick,
            onImportClick = onImportBackupClick,
            onConfirmImport = onConfirmImport,
            onDismissImportPreview = onDismissImportPreview,
            onDismissStatus = onDismissBackupStatus,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            state = sleepTimerState,
            onStart = { minutes ->
                onSleepTimerStart(minutes)
                showSleepTimerSheet = false
            },
            onCancel = {
                onSleepTimerCancel()
                showSleepTimerSheet = false
            },
            onDismiss = { showSleepTimerSheet = false },
        )
    }
}

@Composable
private fun SettingsMainContent(
    appVersion: String,
    themeMode: ThemeMode,
    homeRecommendationsSettings: HomeRecommendationsSettings,
    equalizerState: EqualizerState,
    crossfadeDurationSeconds: Int,
    sleepTimerState: SleepTimerState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onShowRecommendedPlaylistsChange: (Boolean) -> Unit,
    onRecentAddedWindowChange: (RecentAddedWindow) -> Unit,
    onShowNewTrackBadgeChange: (Boolean) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    onSleepTimerClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onMusicScanClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = scaffoldContentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = bottomPadding() + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            SettingsSection(
                title = stringResource(SettingsR.string.settings_section_app_config),
                iconRes = R.drawable.ic_theme,
            ) {
                ThemeModeSelector(
                    selectedThemeMode = themeMode,
                    onThemeModeSelected = onThemeModeChange,
                )
                SettingsDivider()
                SettingsValueRow(title = stringResource(SettingsR.string.settings_notifications_title))
                SettingsDivider()
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_music_scan_title),
                    supportingText = stringResource(SettingsR.string.settings_music_scan_supporting),
                    onClick = onMusicScanClick,
                )
            }
        }
        item {
            SettingsSection(
                title = stringResource(SettingsR.string.settings_section_home_recommendations),
                iconRes = R.drawable.ic_playlist,
            ) {
                SettingsToggleRow(
                    title = stringResource(SettingsR.string.settings_recommended_playlists_title),
                    supportingText = stringResource(SettingsR.string.settings_recommended_playlists_supporting),
                    checked = homeRecommendationsSettings.showRecommendedPlaylists,
                    onCheckedChange = onShowRecommendedPlaylistsChange,
                )
                SettingsDivider()
                RecentAddedWindowSelector(
                    selected = homeRecommendationsSettings.recentAddedWindow,
                    onSelected = onRecentAddedWindowChange,
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = stringResource(SettingsR.string.settings_new_track_badge_title),
                    supportingText = stringResource(SettingsR.string.settings_new_track_badge_supporting),
                    checked = homeRecommendationsSettings.showNewTrackBadge,
                    onCheckedChange = onShowNewTrackBadgeChange,
                )
            }
        }
        item {
            SettingsSection(
                title = stringResource(SettingsR.string.settings_section_audio),
                iconRes = R.drawable.ic_audio,
            ) {
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_sleep_timer_title),
                    supportingText = when (sleepTimerState) {
                        SleepTimerState.Inactive ->
                            stringResource(SettingsR.string.settings_sleep_timer_supporting)
                        is SleepTimerState.Active -> stringResource(
                            SettingsR.string.settings_sleep_timer_remaining,
                            formatSleepTimerRemaining(sleepTimerState.remainingMillis),
                        )
                    },
                    value = if (sleepTimerState is SleepTimerState.Active) {
                        stringResource(SettingsR.string.settings_sleep_timer_active)
                    } else {
                        null
                    },
                    onClick = onSleepTimerClick,
                )
                SettingsDivider()
                CrossfadeDurationSlider(
                    durationSeconds = crossfadeDurationSeconds,
                    onDurationChange = onCrossfadeDurationChange,
                )
                SettingsDivider()
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_equalizer_title),
                    supportingText = stringResource(SettingsR.string.settings_equalizer_supporting),
                    value = equalizerState.statusLabel(),
                    onClick = onEqualizerClick,
                )
            }
        }
        item {
            SettingsSection(
                title = stringResource(SettingsR.string.settings_section_data_recovery),
                iconRes = R.drawable.ic_line_chart,
            ) {
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_backup_title),
                    supportingText = stringResource(SettingsR.string.settings_backup_supporting),
                    onClick = onBackupClick,
                )
            }
        }
        item {
            SettingsSection(
                title = stringResource(SettingsR.string.settings_section_about),
                iconRes = R.drawable.ic_info,
            ) {
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_about_title),
                    supportingText = stringResource(SettingsR.string.settings_about_supporting),
                    value = appVersion,
                    onClick = onAboutClick,
                )
                SettingsDivider()
                SettingsValueRow(title = stringResource(SettingsR.string.settings_privacy_policy_title))
            }
        }
    }
}

@Composable
private fun CrossfadeDurationSlider(
    durationSeconds: Int,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember(durationSeconds) { mutableFloatStateOf(durationSeconds.toFloat()) }
    val selectedSeconds = sliderValue.toInt().coerceIn(
        MIN_CROSSFADE_DURATION_SECONDS,
        MAX_CROSSFADE_DURATION_SECONDS,
    )
    val valueLabel = if (selectedSeconds == 0) {
        stringResource(SettingsR.string.settings_crossfade_value_off)
    } else {
        stringResource(SettingsR.string.settings_crossfade_value_seconds, selectedSeconds)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsRowText(
                title = stringResource(SettingsR.string.settings_crossfade_title),
                supportingText = if (selectedSeconds == 0) {
                    stringResource(SettingsR.string.settings_crossfade_supporting_inactive)
                } else {
                    stringResource(SettingsR.string.settings_crossfade_supporting_active)
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it.toInt().toFloat() },
            onValueChangeFinished = { onDurationChange(selectedSeconds) },
            valueRange = MIN_CROSSFADE_DURATION_SECONDS.toFloat()..
                MAX_CROSSFADE_DURATION_SECONDS.toFloat(),
            steps = MAX_CROSSFADE_DURATION_SECONDS - MIN_CROSSFADE_DURATION_SECONDS - 1,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = valueLabel },
        )
    }
}

@Composable
private fun ThemeModeSelector(
    selectedThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        SettingsValueRow(
            title = stringResource(SettingsR.string.settings_theme_title),
            value = stringResource(selectedThemeMode.labelRes),
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { themeMode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = themeMode == selectedThemeMode,
                                onClick = {
                                    onThemeModeSelected(themeMode)
                                    expanded = false
                                },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = themeMode == selectedThemeMode,
                            onClick = null,
                        )
                        SettingsRowText(
                            title = stringResource(themeMode.labelRes),
                            supportingText = stringResource(themeMode.descriptionRes),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentAddedWindowSelector(
    selected: RecentAddedWindow,
    onSelected: (RecentAddedWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        SettingsValueRow(
            title = stringResource(SettingsR.string.settings_recent_added_title),
            supportingText = stringResource(SettingsR.string.settings_recent_added_supporting),
            value = stringResource(selected.labelRes, selected.days),
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecentAddedWindow.entries.forEach { window ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = window == selected,
                                onClick = {
                                    onSelected(window)
                                    expanded = false
                                },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = window == selected,
                            onClick = null,
                        )
                        SettingsRowText(
                            title = stringResource(window.labelRes),
                            supportingText = null,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@get:StringRes
private val RecentAddedWindow.labelRes: Int
    get() = SettingsR.string.settings_recent_added_window_days

@get:StringRes
private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.System -> SettingsR.string.settings_theme_system
        ThemeMode.Light -> SettingsR.string.settings_theme_light
        ThemeMode.Dark -> SettingsR.string.settings_theme_dark
    }

@get:StringRes
private val ThemeMode.descriptionRes: Int
    get() = when (this) {
        ThemeMode.System -> SettingsR.string.settings_theme_system_description
        ThemeMode.Light -> SettingsR.string.settings_theme_light_description
        ThemeMode.Dark -> SettingsR.string.settings_theme_dark_description
    }

@Composable
private fun EqualizerState.statusLabel(): String = when {
    !isAvailable -> stringResource(SettingsR.string.settings_equalizer_status_unavailable)
    enabled -> selectedPresetName ?: stringResource(SettingsR.string.settings_equalizer_status_active)
    else -> stringResource(SettingsR.string.settings_equalizer_status_disabled)
}

private enum class SettingsDestination {
    Main,
    About,
    Equalizer,
    MusicScan,
    ScanFolders,
    Backup,
}
