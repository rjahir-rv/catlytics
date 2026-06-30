package com.catlytics.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
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
    onThemeModeChange: (ThemeMode) -> Unit,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    onEqualizerModeChange: (EqualizerMode) -> Unit,
    onEqualizerPresetSelected: (EqualizerPreset) -> Unit,
    onCustomBandLevelChange: (Short, Int, Boolean) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {}
) {
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.Main) }

    LaunchedEffect(destination) {
        when (destination) {
            SettingsDestination.Main -> {
                onTopBarTitleChange("Ajustes")
                onTopBarBackActionChange(null)
            }
            SettingsDestination.Equalizer -> {
                onTopBarTitleChange("Ecualizador")
                onTopBarBackActionChange { destination = SettingsDestination.Main }
            }
        }
    }

    when (destination) {
        SettingsDestination.Main -> SettingsMainContent(
            appVersion = appVersion,
            themeMode = themeMode,
            equalizerState = equalizerState,
            onThemeModeChange = onThemeModeChange,
            onEqualizerClick = { destination = SettingsDestination.Equalizer },
            bottomPadding = bottomPadding,
            modifier = modifier,
        )
        SettingsDestination.Equalizer -> EqualizerSettingsContent(
            equalizerState = equalizerState,
            onEqualizerEnabledChange = onEqualizerEnabledChange,
            onEqualizerModeChange = onEqualizerModeChange,
            onEqualizerPresetSelected = onEqualizerPresetSelected,
            onCustomBandLevelChange = onCustomBandLevelChange,
            bottomPadding = bottomPadding,
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsMainContent(
    appVersion: String,
    themeMode: ThemeMode,
    equalizerState: EqualizerState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onEqualizerClick: () -> Unit,
    bottomPadding: () -> Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 24.dp,
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
                SettingsValueRow(
                    title = "Color de acento",
                    value = "Rosa",
                )
                SettingsDivider()
                SettingsValueRow(title = "Notificaciones")
            }
        }
        item {
            SettingsSection(
                title = "Audio",
                iconRes = R.drawable.ic_audio,
            ) {
                SettingsValueRow(
                    title = "Crossfade",
                    value = "4 s",
                )
                SettingsDivider()
                SettingsValueRow(
                    title = "Ecualizador",
                    supportingText = "Presets del dispositivo",
                    value = equalizerState.statusLabel,
                    onClick = onEqualizerClick,
                )
                SettingsDivider()
                SettingsValueRow(
                    title = "Calidad de audio",
                    supportingText = "Reproducción local",
                    value = "Sin perdida",
                )
            }
        }
        item {
            SettingsSection(
                title = "Acerca de",
                iconRes = R.drawable.ic_info,
            ) {
                SettingsValueRow(
                    title = "Version",
                    value = appVersion,
                    showChevron = false,
                )
                SettingsDivider()
                SettingsValueRow(title = "Términos del servicio")
                SettingsDivider()
                SettingsValueRow(title = "Política de privacidad")
            }
        }
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
    Equalizer,
}

