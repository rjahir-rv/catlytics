package com.catlytics.feature.settings.impl

import androidx.annotation.RawRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.core.designsystem.layout.LocalPaneChrome
import com.catlytics.feature.settings.api.SettingsRoute

/** @param librariesRes `R.raw.aboutlibraries` de `:app`, generado por el plugin de AboutLibraries. */
fun EntryProviderScope<NavKey>.settingsEntry(
    appVersion: String,
    @RawRes librariesRes: Int,
    hasAudioPermission: () -> Boolean = { true },
    onRequestAudioPermission: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {},
) {
    entry<SettingsRoute> {
        SettingsRoute(
            appVersion = appVersion,
            librariesRes = librariesRes,
            hasAudioPermission = hasAudioPermission(),
            onRequestAudioPermission = onRequestAudioPermission,
            bottomPadding = bottomPadding,
            onTopBarTitleChange = onTopBarTitleChange,
            onTopBarBackActionChange = onTopBarBackActionChange,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
        )
    }
}
