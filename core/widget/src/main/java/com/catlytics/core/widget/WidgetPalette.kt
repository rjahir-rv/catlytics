package com.catlytics.core.widget

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.catlytics.core.designsystem.theme.catlyticsColorScheme
import com.catlytics.core.model.ThemeMode
import androidx.glance.color.ColorProvider as DayNightColorProvider

internal data class CassetteColors(
    val body: Color,
    val screw: Color,
    val window: Color,
    val reel: Color,
    val labelPaper: Color,
    val labelStripe: Color,
    val onLabelStripe: Color,
    val stripeLedOff: Color,
    val labelText: Color,
    val labelSubtext: Color,
    val key: Color,
    val keyIcon: Color,
    val accentKey: Color,
    val onAccentKey: Color,
    val ledOn: Color,
    val ledOff: Color,
)

internal fun cassetteColors(scheme: ColorScheme, darkTheme: Boolean): CassetteColors = CassetteColors(
    body = scheme.surfaceContainerHigh,
    screw = scheme.outline,
    // En claro, ventana oscura con bobinas claras; en oscuro, negro OLED con bobinas grises.
    window = if (darkTheme) scheme.surfaceContainerLowest else scheme.inverseSurface,
    reel = if (darkTheme) scheme.onSurfaceVariant else scheme.surfaceContainerHighest,
    labelPaper = if (darkTheme) scheme.surfaceContainerHighest else scheme.surfaceContainerLowest,
    labelStripe = scheme.primary,
    onLabelStripe = scheme.onPrimary,
    stripeLedOff = scheme.primaryContainer,
    labelText = scheme.onSurface,
    labelSubtext = scheme.onSurfaceVariant,
    key = if (darkTheme) scheme.surfaceContainerHighest else scheme.tertiaryContainer,
    keyIcon = if (darkTheme) scheme.onSurface else scheme.onTertiary,
    accentKey = scheme.primary,
    onAccentKey = scheme.onPrimary,
    ledOn = scheme.primary,
    ledOff = scheme.outlineVariant,
)

internal data class WidgetPalette(
    val body: ColorProvider,
    val screw: ColorProvider,
    val window: ColorProvider,
    val reel: ColorProvider,
    val labelPaper: ColorProvider,
    val labelStripe: ColorProvider,
    val onLabelStripe: ColorProvider,
    val stripeLedOff: ColorProvider,
    val labelText: ColorProvider,
    val labelSubtext: ColorProvider,
    val key: ColorProvider,
    val keyIcon: ColorProvider,
    val accentKey: ColorProvider,
    val onAccentKey: ColorProvider,
    val ledOn: ColorProvider,
    val ledOff: ColorProvider,
)

/**
 * Respeta el tema elegido en la app: con "Sistema" cada color lleva su variante día/noche y el
 * launcher la resuelve; con Claro u Oscuro se fija un único color.
 */
internal fun widgetPalette(themeMode: ThemeMode): WidgetPalette {
    val light = cassetteColors(catlyticsColorScheme(darkTheme = false), darkTheme = false)
    val dark = cassetteColors(catlyticsColorScheme(darkTheme = true), darkTheme = true)
    fun color(slot: CassetteColors.() -> Color): ColorProvider = when (themeMode) {
        ThemeMode.System -> DayNightColorProvider(day = light.slot(), night = dark.slot())
        ThemeMode.Light -> ColorProvider(light.slot())
        ThemeMode.Dark -> ColorProvider(dark.slot())
    }
    return WidgetPalette(
        body = color { body },
        screw = color { screw },
        window = color { window },
        reel = color { reel },
        labelPaper = color { labelPaper },
        labelStripe = color { labelStripe },
        onLabelStripe = color { onLabelStripe },
        stripeLedOff = color { stripeLedOff },
        labelText = color { labelText },
        labelSubtext = color { labelSubtext },
        key = color { key },
        keyIcon = color { keyIcon },
        accentKey = color { accentKey },
        onAccentKey = color { onAccentKey },
        ledOn = color { ledOn },
        ledOff = color { ledOff },
    )
}
