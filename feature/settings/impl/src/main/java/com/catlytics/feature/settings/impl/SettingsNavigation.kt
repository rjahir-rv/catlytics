package com.catlytics.feature.settings.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.settings.api.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry(
    appVersion: String,
) {
    entry<SettingsRoute> {
        SettingsRoute(appVersion = appVersion)
    }
}
