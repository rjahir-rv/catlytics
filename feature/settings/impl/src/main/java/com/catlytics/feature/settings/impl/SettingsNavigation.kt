package com.catlytics.feature.settings.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.settings.api.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry(
    appVersion: String,
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {},
    contentPadding: () -> androidx.compose.foundation.layout.PaddingValues = { androidx.compose.foundation.layout.PaddingValues(0.dp) },
) {
    entry<SettingsRoute> {
        Box(modifier = Modifier.padding(contentPadding())) {
            SettingsRoute(
                appVersion = appVersion,
                bottomPadding = bottomPadding,
                onTopBarTitleChange = onTopBarTitleChange,
                onTopBarBackActionChange = onTopBarBackActionChange,
            )
        }
    }
}
