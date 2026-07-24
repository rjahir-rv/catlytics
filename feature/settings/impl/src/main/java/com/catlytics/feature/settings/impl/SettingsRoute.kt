package com.catlytics.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SettingsRoute(
    appVersion: String,
    hasAudioPermission: Boolean = true,
    onRequestAudioPermission: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
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
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
        onTopBarTitleChange = onTopBarTitleChange,
        onTopBarBackActionChange = onTopBarBackActionChange,
    )
}
