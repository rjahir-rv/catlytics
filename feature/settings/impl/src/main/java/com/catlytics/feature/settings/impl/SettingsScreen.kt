package com.catlytics.feature.settings.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.ThemeMode
import com.catlytics.feature.settings.impl.components.SettingsDivider
import com.catlytics.feature.settings.impl.components.SettingsRowText
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.feature.settings.impl.components.SettingsValueRow
import com.catlytics.feature.settings.impl.equalizer.EqualizerSettingsContent

@Composable
internal fun SettingsScreen(
    appVersion: String,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    equalizerState: EqualizerState,
    crossfadeDurationSeconds: Int,
    sleepTimerState: SleepTimerState,
    libraryFolders: List<LibraryFolder>,
    musicScanSettings: MusicScanSettings,
    musicScanStatus: MusicScanStatus,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
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
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {}
) {
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.Main) }
    var showSleepTimerSheet by rememberSaveable { mutableStateOf(false) }

    fun navigateBack() {
        destination = when (destination) {
            SettingsDestination.ScanFolders -> SettingsDestination.MusicScan
            SettingsDestination.About,
            SettingsDestination.Equalizer,
            SettingsDestination.MusicScan -> SettingsDestination.Main
            SettingsDestination.Main -> SettingsDestination.Main
        }
    }

    BackHandler(enabled = destination != SettingsDestination.Main, onBack = ::navigateBack)

    LaunchedEffect(destination) {
        when (destination) {
            SettingsDestination.Main -> {
                onTopBarTitleChange("Ajustes")
                onTopBarBackActionChange(null)
            }
            SettingsDestination.Equalizer -> {
                onTopBarTitleChange("Ecualizador")
                onTopBarBackActionChange(::navigateBack)
            }
            SettingsDestination.About -> {
                onTopBarTitleChange("Acerca de Catlytics")
                onTopBarBackActionChange(::navigateBack)
            }
            SettingsDestination.MusicScan -> {
                onTopBarTitleChange("Escanear música")
                onTopBarBackActionChange(::navigateBack)
            }
            SettingsDestination.ScanFolders -> {
                onTopBarTitleChange("Carpetas")
                onTopBarBackActionChange(::navigateBack)
            }
        }
    }

    when (destination) {
        SettingsDestination.Main -> SettingsMainContent(
            appVersion = appVersion,
            themeMode = themeMode,
            equalizerState = equalizerState,
            crossfadeDurationSeconds = crossfadeDurationSeconds,
            sleepTimerState = sleepTimerState,
            onThemeModeChange = onThemeModeChange,
            onCrossfadeDurationChange = onCrossfadeDurationChange,
            onSleepTimerClick = { showSleepTimerSheet = true },
            onEqualizerClick = { destination = SettingsDestination.Equalizer },
            onMusicScanClick = { destination = SettingsDestination.MusicScan },
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
    equalizerState: EqualizerState,
    crossfadeDurationSeconds: Int,
    sleepTimerState: SleepTimerState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    onSleepTimerClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onMusicScanClick: () -> Unit,
    onAboutClick: () -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
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
                title = "Configuración de la app",
                iconRes = R.drawable.ic_theme,
            ) {
                ThemeModeSelector(
                    selectedThemeMode = themeMode,
                    onThemeModeSelected = onThemeModeChange,
                )
                SettingsDivider()
                SettingsValueRow(title = "Notificaciones")
                SettingsDivider()
                SettingsValueRow(
                    title = "Escanear música",
                    supportingText = "Carpetas y filtros para encontrar música local",
                    onClick = onMusicScanClick,
                )
            }
        }
        item {
            SettingsSection(
                title = "Audio",
                iconRes = R.drawable.ic_audio,
            ) {
                SettingsValueRow(
                    title = "Temporizador de sueño",
                    supportingText = when (sleepTimerState) {
                        SleepTimerState.Inactive -> "Pausa la música después de un tiempo"
                        is SleepTimerState.Active -> "Quedan ${formatSleepTimerRemaining(sleepTimerState.remainingMillis)}"
                    },
                    value = if (sleepTimerState is SleepTimerState.Active) "Activo" else null,
                    onClick = onSleepTimerClick,
                )
                SettingsDivider()
                CrossfadeDurationSlider(
                    durationSeconds = crossfadeDurationSeconds,
                    onDurationChange = onCrossfadeDurationChange,
                )
                SettingsDivider()
                SettingsValueRow(
                    title = "Ecualizador",
                    supportingText = "Presets del dispositivo",
                    value = equalizerState.statusLabel,
                    onClick = onEqualizerClick,
                )
            }
        }
        item {
            SettingsSection(
                title = "Acerca de",
                iconRes = R.drawable.ic_info,
            ) {
                SettingsValueRow(
                    title = "Acerca de Catlytics",
                    supportingText = "Información de la app y código fuente",
                    value = appVersion,
                    onClick = onAboutClick,
                )
                SettingsDivider()
                SettingsValueRow(title = "Política de privacidad")
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
    val valueLabel = if (selectedSeconds == 0) "Desactivado" else "$selectedSeconds s"

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
                title = "Crossfade",
                supportingText = if (selectedSeconds == 0) {
                    "Se usará reproducción sin pausas automática"
                } else {
                    "Mezcla al terminar una canción"
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
            title = "Tema",
            value = selectedThemeMode.label,
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            SettingsDivider()
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { themeMode ->
                    ThemeModeOption(
                        title = themeMode.label,
                        supportingText = themeMode.supportingText,
                        selected = selectedThemeMode == themeMode,
                        onClick = {
                            onThemeModeSelected(themeMode)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    title: String,
    supportingText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
        RadioButton(
            selected = selected,
            onClick = null,
        )
        SettingsRowText(
            title = title,
            supportingText = supportingText,
            modifier = Modifier.weight(1f),
        )
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.System -> "Sistema"
        ThemeMode.Light -> "Claro"
        ThemeMode.Dark -> "Oscuro"
    }

private val ThemeMode.supportingText: String
    get() = when (this) {
        ThemeMode.System -> "Usar el tema configurado en el dispositivo"
        ThemeMode.Light -> "Usar siempre el tema claro"
        ThemeMode.Dark -> "Usar siempre el tema oscuro"
    }

private val EqualizerState.statusLabel: String
    get() = when {
        !isAvailable -> "No disponible"
        enabled -> selectedPresetName ?: "Activo"
        else -> "Desactivado"
    }

private enum class SettingsDestination {
    Main,
    About,
    Equalizer,
    MusicScan,
    ScanFolders,
}
