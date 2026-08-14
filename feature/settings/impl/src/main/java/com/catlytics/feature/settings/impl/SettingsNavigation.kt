package com.catlytics.feature.settings.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.settings.api.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry(
    appVersion: String,
    hasAudioPermission: () -> Boolean = { true },
    onRequestAudioPermission: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {},
    scaffoldContentPadding: () -> PaddingValues = { PaddingValues(0.dp) },
) {
    entry<SettingsRoute> {
        SettingsRoute(
            appVersion = appVersion,
            hasAudioPermission = hasAudioPermission(),
            onRequestAudioPermission = onRequestAudioPermission,
            bottomPadding = bottomPadding,
            onTopBarTitleChange = onTopBarTitleChange,
            onTopBarBackActionChange = onTopBarBackActionChange,
            scaffoldContentPadding = scaffoldContentPadding(),
        )
    }
}
