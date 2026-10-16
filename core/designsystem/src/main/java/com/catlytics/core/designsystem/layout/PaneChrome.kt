package com.catlytics.core.designsystem.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
class PaneChrome(
    val contentPadding: PaddingValues,
    val collapsedFraction: () -> Float,
) {
    companion object {
        val None = PaneChrome(contentPadding = PaddingValues(0.dp), collapsedFraction = { 0f })
    }
}

val LocalPaneChrome = compositionLocalOf { PaneChrome.None }
