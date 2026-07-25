package com.catlytics.feature.settings.impl

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun SettingsRoute(
    appVersion: String,
    hasAudioPermission: Boolean = true,
    onRequestAudioPermission: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val libraryFolders by viewModel.libraryFolders.collectAsStateWithLifecycle()
    val musicScanSettings by viewModel.musicScanSettings.collectAsStateWithLifecycle()
    val musicScanStatus by viewModel.musicScanStatus.collectAsStateWithLifecycle()
    val statisticsBackupSummary by viewModel.statisticsBackupSummary.collectAsStateWithLifecycle()
    val statisticsBackupStatus by viewModel.statisticsBackupStatus.collectAsStateWithLifecycle()
    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportStatisticsBackup(uri.toString(), appVersion)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.loadImportPreview(uri.toString())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshEqualizer()
    }

    SettingsScreen(
        appVersion = appVersion,
        themeMode = themeMode,
        equalizerState = equalizerState,
        crossfadeDurationSeconds = crossfadeDurationSeconds,
        sleepTimerState = sleepTimerState,
        libraryFolders = libraryFolders,
        musicScanSettings = musicScanSettings,
        musicScanStatus = musicScanStatus,
        hasAudioPermission = hasAudioPermission,
        statisticsBackupSummary = statisticsBackupSummary,
        statisticsBackupStatus = statisticsBackupStatus,
        importPreview = importPreview,
        onRequestAudioPermission = onRequestAudioPermission,
        onThemeModeChange = viewModel::setThemeMode,
        onCrossfadeDurationChange = viewModel::setCrossfadeDurationSeconds,
        onSleepTimerStart = viewModel::startSleepTimer,
        onSleepTimerCancel = viewModel::cancelSleepTimer,
        onFolderVisibilityChange = viewModel::setFolderVisible,
        onMusicScanDurationFilterChange = viewModel::setMusicScanDurationFilter,
        onMusicScanSizeFilterChange = viewModel::setMusicScanSizeFilter,
        onScanMusic = viewModel::scanMusic,
        onEqualizerEnabledChange = viewModel::setEqualizerEnabled,
        onEqualizerModeChange = viewModel::setEqualizerMode,
        onEqualizerPresetSelected = viewModel::selectEqualizerPreset,
        onCustomBandLevelChange = viewModel::setCustomBandLevel,
        onExportStatisticsClick = {
            val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            exportLauncher.launch("catlytics-stats-$date.json")
        },
        onImportStatisticsClick = {
            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onConfirmImport = viewModel::confirmImport,
        onDismissImportPreview = viewModel::dismissImportPreview,
        onDismissStatisticsBackupStatus = viewModel::dismissStatisticsBackupStatus,
        bottomPadding = bottomPadding,
        onTopBarTitleChange = onTopBarTitleChange,
        onTopBarBackActionChange = onTopBarBackActionChange,
    )
}
