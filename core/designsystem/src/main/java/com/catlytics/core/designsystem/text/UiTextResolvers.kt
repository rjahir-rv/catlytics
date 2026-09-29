package com.catlytics.core.designsystem.text

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, *formatArgs())
}


fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Resource -> resources.getString(id, *args.toTypedArray())
    is UiText.Plural -> resources.getQuantityString(id, count, *formatArgs())
}

fun UiText.resolve(context: Context): String = resolve(context.resources)

private fun UiText.Plural.formatArgs(): Array<Any> =
    if (args.isEmpty()) arrayOf(count) else args.toTypedArray()
