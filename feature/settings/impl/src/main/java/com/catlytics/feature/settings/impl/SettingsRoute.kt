package com.catlytics.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SettingsRoute(
    appVersion: String,
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshEqualizer()
    }

    SettingsScreen(
        appVersion = appVersion,
        themeMode = themeMode,
        equalizerState = equalizerState,
        onThemeModeChange = viewModel::setThemeMode,
        onEqualizerEnabledChange = viewModel::setEqualizerEnabled,
        onEqualizerModeChange = viewModel::setEqualizerMode,
        onEqualizerPresetSelected = viewModel::selectEqualizerPreset,
        onCustomBandLevelChange = viewModel::setCustomBandLevel,
        bottomPadding = bottomPadding,
        onTopBarTitleChange = onTopBarTitleChange,
        onTopBarBackActionChange = onTopBarBackActionChange,
    )
}
