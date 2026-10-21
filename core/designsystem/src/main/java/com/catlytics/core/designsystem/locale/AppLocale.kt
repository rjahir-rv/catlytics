package com.catlytics.core.designsystem.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.catlytics.core.model.AppLanguage
import java.util.Locale

fun AppLanguage.resolveLocale(): Locale = when (this) {
    AppLanguage.System -> systemLocale()
    AppLanguage.Spanish -> Locale.forLanguageTag("es")
    AppLanguage.English -> Locale.forLanguageTag("en")
}

fun systemLocale(): Locale {
    val locales = Resources.getSystem().configuration.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}

fun Context.withAppLocale(locale: Locale): Context {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    return createConfigurationContext(configuration)
}
