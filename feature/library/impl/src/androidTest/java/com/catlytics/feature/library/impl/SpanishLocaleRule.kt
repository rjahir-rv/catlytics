package com.catlytics.feature.library.impl

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// Las aserciones usan el texto en español; el idioma por app también llega a diálogos y sheets.
class SpanishLocaleRule : TestWatcher() {
    private var previous: LocaleList? = null

    override fun starting(description: Description) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val localeManager = localeManager()
        previous = localeManager.applicationLocales
        localeManager.applicationLocales = LocaleList.forLanguageTags("es")
    }

    override fun finished(description: Description) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        previous?.let { localeManager().applicationLocales = it }
    }

    private fun localeManager(): LocaleManager =
        InstrumentationRegistry.getInstrumentation().targetContext.getSystemService(LocaleManager::class.java)
}
