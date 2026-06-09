package com.catlytics.feature.settings.impl

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.ThemeMode

@Composable
internal fun SettingsRoute(
    appVersion: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    SettingsScreen(
        appVersion = appVersion,
        themeMode = themeMode,
        onThemeModeChange = viewModel::setThemeMode,
    )
}

@Composable
internal fun SettingsScreen(
    appVersion: String,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
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
                    value = "Personalizado",
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
private fun SettingsSection(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        SettingsDivider()
        content()
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

@Composable
private fun SettingsValueRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = showChevron, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRowText(
            title = title,
            supportingText = supportingText,
            modifier = Modifier.weight(1f),
        )
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsRowText(
    title: String,
    supportingText: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    CatlyticsTheme {
        SettingsScreen(
            appVersion = "0.0.1",
            themeMode = ThemeMode.System,
            onThemeModeChange = {},
        )
    }
}
