package com.catlytics.feature.settings.impl

import androidx.activity.compose.BackHandler
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.format.SleepTimerFormat
import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import com.catlytics.core.designsystem.layout.deferredPadding
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.MAX_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.MIN_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.model.AppLanguage
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
import com.catlytics.core.model.HomeRecommendationsSettings
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.ThemeMode
import com.catlytics.core.navigation.navigationBackTransition
import com.catlytics.core.navigation.navigationForwardTransition
import com.catlytics.feature.settings.impl.components.SettingsAnimatedValue
import com.catlytics.feature.settings.impl.components.SettingsDivider
import com.catlytics.feature.settings.impl.components.SettingsRowText
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.feature.settings.impl.components.SettingsToggleRow
import com.catlytics.feature.settings.impl.components.SettingsValueRow
import com.catlytics.feature.settings.impl.edits.MetadataEditsRoute
import com.catlytics.feature.settings.impl.equalizer.EqualizerSettingsContent
import com.catlytics.feature.settings.impl.R as SettingsR

@Composable
internal fun SettingsScreen(
    appVersion: String,
    @RawRes librariesRes: Int,
    uiState: SettingsUiState,
    hasAudioPermission: Boolean,
    modifier: Modifier = Modifier,
    onRequestAudioPermission: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
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
    var hasPlayedEntrance by rememberSaveable { mutableStateOf(false) }

    fun navigateBack() {
        destination = when (destination) {
            SettingsDestination.ScanFolders -> SettingsDestination.MusicScan
            SettingsDestination.Licenses -> SettingsDestination.About
            SettingsDestination.About,
            SettingsDestination.Equalizer,
            SettingsDestination.MusicScan,
            SettingsDestination.Backup,
            SettingsDestination.MetadataEdits,
            -> SettingsDestination.Main
            SettingsDestination.Main -> SettingsDestination.Main
        }
    }

    BackHandler(enabled = destination != SettingsDestination.Main, onBack = ::navigateBack)

    val mainTitle = stringResource(SettingsR.string.settings_title)
    val equalizerTitle = stringResource(SettingsR.string.settings_equalizer_title)
    val aboutTitle = stringResource(SettingsR.string.settings_about_title)
    val licensesTitle = stringResource(SettingsR.string.settings_licenses_title)
    val musicScanTitle = stringResource(SettingsR.string.settings_music_scan_title)
    val foldersTitle = stringResource(SettingsR.string.settings_folders_title)
    val backupTitle = stringResource(SettingsR.string.settings_backup_title)
    val metadataEditsTitle = stringResource(SettingsR.string.settings_edits_title)

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
            SettingsDestination.Licenses -> {
                onTopBarTitleChange(licensesTitle)
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
            SettingsDestination.MetadataEdits -> {
                onTopBarTitleChange(metadataEditsTitle)
                onTopBarBackActionChange(::navigateBack)
            }
        }
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            if (targetState.depth >= initialState.depth) {
                navigationForwardTransition()
            } else {
                navigationBackTransition()
            }
        },
        label = "settingsDestination",
    ) { currentDestination ->
        when (currentDestination) {
            SettingsDestination.Main -> SettingsMainContent(
                appVersion = appVersion,
                animateEntrance = !hasPlayedEntrance,
                onEntranceStarted = { hasPlayedEntrance = true },
                themeMode = uiState.themeMode,
                appLanguage = uiState.appLanguage,
                homeRecommendationsSettings = uiState.homeRecommendationsSettings,
                equalizerState = uiState.equalizerState,
                crossfadeDurationSeconds = uiState.crossfadeDurationSeconds,
                sleepTimerState = uiState.sleepTimerState,
                onThemeModeChange = onThemeModeChange,
                onAppLanguageChange = onAppLanguageChange,
                onShowRecommendedPlaylistsChange = onShowRecommendedPlaylistsChange,
                onRecentAddedWindowChange = onRecentAddedWindowChange,
                onShowNewTrackBadgeChange = onShowNewTrackBadgeChange,
                onCrossfadeDurationChange = onCrossfadeDurationChange,
                onSleepTimerClick = { showSleepTimerSheet = true },
                onEqualizerClick = { destination = SettingsDestination.Equalizer },
                onMusicScanClick = { destination = SettingsDestination.MusicScan },
                onBackupClick = { destination = SettingsDestination.Backup },
                onMetadataEditsClick = { destination = SettingsDestination.MetadataEdits },
                onAboutClick = { destination = SettingsDestination.About },
                bottomPadding = bottomPadding,
                scaffoldContentPadding = scaffoldContentPadding,
                modifier = modifier,
            )
            SettingsDestination.About -> AboutSettingsContent(
                appVersion = appVersion,
                onMetadataEditsClick = { destination = SettingsDestination.MetadataEdits },
                onLicensesClick = { destination = SettingsDestination.Licenses },
                bottomPadding = bottomPadding,
                scaffoldContentPadding = scaffoldContentPadding,
                modifier = modifier,
            )
            SettingsDestination.Licenses -> LicensesSettingsContent(
                librariesRes = librariesRes,
                bottomPadding = bottomPadding,
                scaffoldContentPadding = scaffoldContentPadding,
                modifier = modifier,
            )
            SettingsDestination.Equalizer -> EqualizerSettingsContent(
                equalizerState = uiState.equalizerState,
                onEqualizerEnabledChange = onEqualizerEnabledChange,
                onEqualizerModeChange = onEqualizerModeChange,
                onEqualizerPresetSelected = onEqualizerPresetSelected,
                onCustomBandLevelChange = onCustomBandLevelChange,
                bottomPadding = bottomPadding,
                scaffoldContentPadding = scaffoldContentPadding,
                modifier = modifier,
            )
            SettingsDestination.MusicScan -> MusicScanSettingsContent(
                folders = uiState.libraryFolders,
                settings = uiState.musicScanSettings,
                scanStatus = uiState.musicScanStatus,
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
                folders = uiState.libraryFolders,
                onFolderVisibilityChange = onFolderVisibilityChange,
                bottomPadding = bottomPadding,
                scaffoldContentPadding = scaffoldContentPadding,
                modifier = modifier,
            )
            SettingsDestination.Backup -> UnifiedBackupContent(
                summary = uiState.unifiedBackupSummary,
                operationStatus = uiState.unifiedBackupStatus,
                importPreview = uiState.unifiedImportPreview,
                onExportClick = onExportBackupClick,
                onImportClick = onImportBackupClick,
                onConfirmImport = onConfirmImport,
                onDismissImportPreview = onDismissImportPreview,
                onDismissStatus = onDismissBackupStatus,
                onViewMetadataEdits = { destination = SettingsDestination.MetadataEdits },
                bottomPadding = bottomPadding,
                scaffoldContentPadding = scaffoldContentPadding,
                modifier = modifier,
            )
            SettingsDestination.MetadataEdits -> MetadataEditsRoute(
                bottomPadding = bottomPadding,
                scaffoldContentPadding = scaffoldContentPadding,
                modifier = modifier,
            )
        }
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            state = uiState.sleepTimerState,
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
    animateEntrance: Boolean,
    onEntranceStarted: () -> Unit,
    themeMode: ThemeMode,
    appLanguage: AppLanguage,
    homeRecommendationsSettings: HomeRecommendationsSettings,
    equalizerState: EqualizerState,
    crossfadeDurationSeconds: Int,
    sleepTimerState: SleepTimerState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onShowRecommendedPlaylistsChange: (Boolean) -> Unit,
    onRecentAddedWindowChange: (RecentAddedWindow) -> Unit,
    onShowNewTrackBadgeChange: (Boolean) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    onSleepTimerClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onMusicScanClick: () -> Unit,
    onBackupClick: () -> Unit,
    onMetadataEditsClick: () -> Unit,
    onAboutClick: () -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onEntranceStarted() }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(CatlyticsLayoutTokens.CardMinColumnWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = deferredPadding(
            start = 20.dp,
            top = { scaffoldContentPadding.calculateTopPadding() + 24.dp },
            end = 20.dp,
            bottom = { bottomPadding() + 80.dp },
        ),
        verticalItemSpacing = 20.dp,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            SettingsAppHeader(
                appVersion = appVersion,
                modifier = Modifier.settingsEntrance(index = 0, animate = animateEntrance),
            )
        }
        item {
            SettingsSection(
                modifier = Modifier.settingsEntrance(index = 1, animate = animateEntrance),
                title = stringResource(SettingsR.string.settings_section_app_config),
                iconRes = R.drawable.ic_theme,
            ) {
                ThemeModeSelector(
                    selectedThemeMode = themeMode,
                    onThemeModeSelected = onThemeModeChange,
                )
                SettingsDivider()
                AppLanguageSelector(
                    selectedLanguage = appLanguage,
                    onLanguageSelected = onAppLanguageChange,
                )
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
                modifier = Modifier.settingsEntrance(index = 2, animate = animateEntrance),
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
                modifier = Modifier.settingsEntrance(index = 3, animate = animateEntrance),
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
                            SleepTimerFormat.formatRemaining(sleepTimerState.remainingMillis),
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
                modifier = Modifier.settingsEntrance(index = 4, animate = animateEntrance),
                title = stringResource(SettingsR.string.settings_section_data_recovery),
                iconRes = R.drawable.ic_line_chart,
            ) {
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_backup_title),
                    supportingText = stringResource(SettingsR.string.settings_backup_supporting),
                    onClick = onBackupClick,
                )
                SettingsDivider()
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_edits_title),
                    supportingText = stringResource(SettingsR.string.settings_edits_supporting),
                    onClick = onMetadataEditsClick,
                )
            }
        }
        item {
            SettingsSection(
                modifier = Modifier.settingsEntrance(index = 5, animate = animateEntrance),
                title = stringResource(SettingsR.string.settings_section_about),
                iconRes = R.drawable.ic_info,
            ) {
                SettingsValueRow(
                    title = stringResource(SettingsR.string.settings_about_title),
                    supportingText = stringResource(SettingsR.string.settings_about_supporting),
                    value = appVersion,
                    onClick = onAboutClick,
                )
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
            SettingsAnimatedValue(value = valueLabel)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it.toInt().toFloat() },
            onValueChangeFinished = { onDurationChange(selectedSeconds) },
            valueRange = MIN_CROSSFADE_DURATION_SECONDS.toFloat()..MAX_CROSSFADE_DURATION_SECONDS.toFloat(),
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsRowText(
            title = stringResource(SettingsR.string.settings_theme_title),
            supportingText = stringResource(selectedThemeMode.descriptionRes),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeMode.entries.forEach { themeMode ->
                ThemePreviewCard(
                    themeMode = themeMode,
                    selected = themeMode == selectedThemeMode,
                    onClick = { onThemeModeSelected(themeMode) },
                    modifier = Modifier.weight(1f),
                )
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
            expanded = expanded,
            onClick = { expanded = !expanded },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
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
                            title = stringResource(window.labelRes, window.days),
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

@Composable
private fun AppLanguageSelector(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        SettingsValueRow(
            title = stringResource(SettingsR.string.settings_language_title),
            supportingText = stringResource(selectedLanguage.descriptionRes),
            value = stringResource(selectedLanguage.labelRes),
            expanded = showDialog,
            onClick = { showDialog = true },
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(SettingsR.string.settings_language_title)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = language == selectedLanguage,
                                    onClick = {
                                        onLanguageSelected(language)
                                        showDialog = false
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = language == selectedLanguage,
                                onClick = null,
                            )
                            SettingsRowText(
                                title = stringResource(language.labelRes),
                                supportingText = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(SettingsR.string.settings_action_cancel))
                }
            },
        )
    }
}

@get:StringRes
private val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.System -> SettingsR.string.settings_language_system
        AppLanguage.Spanish -> SettingsR.string.settings_language_spanish
        AppLanguage.English -> SettingsR.string.settings_language_english
    }

@get:StringRes
private val AppLanguage.descriptionRes: Int
    get() = when (this) {
        AppLanguage.System -> SettingsR.string.settings_language_system_description
        AppLanguage.Spanish -> SettingsR.string.settings_language_spanish_description
        AppLanguage.English -> SettingsR.string.settings_language_english_description
    }

@get:StringRes
private val RecentAddedWindow.labelRes: Int
    get() = SettingsR.string.settings_recent_added_window_days

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

private enum class SettingsDestination(val depth: Int) {
    Main(0),
    About(1),
    Licenses(2),
    Equalizer(1),
    MusicScan(1),
    ScanFolders(2),
    Backup(1),
    MetadataEdits(1),
}
