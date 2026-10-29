package com.catlytics.core.designsystem.text

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources

@Composable
fun UiText.asString(): String = resolve(LocalResources.current)

fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Resource -> resources.getString(id, *args.resolveArgs(resources))
    is UiText.Plural -> resources.getQuantityString(id, count, *formatArgs(resources))
}

fun UiText.resolve(context: Context): String = resolve(context.resources)

private fun UiText.Plural.formatArgs(resources: Resources): Array<Any> =
    if (args.isEmpty()) arrayOf(count) else args.resolveArgs(resources)

private fun List<Any>.resolveArgs(resources: Resources): Array<Any> =
    Array(size) { index -> (this[index] as? UiText)?.resolve(resources) ?: this[index] }
