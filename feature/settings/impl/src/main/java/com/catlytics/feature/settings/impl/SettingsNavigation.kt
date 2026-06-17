package com.catlytics.feature.settings.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.settings.api.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry(
    appVersion: String,
    contentModifier: Modifier = Modifier,
) {
    entry<SettingsRoute> {
        Box(modifier = contentModifier) {
            SettingsRoute(appVersion = appVersion)
        }
    }
}
