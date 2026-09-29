package com.catlytics.core.designsystem.text

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/** Resuelve el texto dentro de un Composable (configuration-aware). */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, *formatArgs())
}

/**
 * Resuelve el texto fuera de Compose con un [Resources] configuration-aware
 * (p. ej. `LocalResources.current`), evitando valores obsoletos si cambia la configuración.
 */
fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Resource -> resources.getString(id, *args.toTypedArray())
    is UiText.Plural -> resources.getQuantityString(id, count, *formatArgs())
}

/** Resuelve el texto con un [Context] (para `Toast`, `Notification`, etc.). */
fun UiText.resolve(context: Context): String = resolve(context.resources)

private fun UiText.Plural.formatArgs(): Array<Any> =
    if (args.isEmpty()) arrayOf(count) else args.toTypedArray()
