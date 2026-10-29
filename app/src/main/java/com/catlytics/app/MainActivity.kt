package com.catlytics.app

import android.app.LocaleManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.catlytics.app.ui.CatlyticsApp
import com.catlytics.app.ui.ThemeViewModel
import com.catlytics.core.designsystem.locale.resolveLocale
import com.catlytics.core.designsystem.locale.withAppLocale
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.model.AppLanguage
import com.catlytics.core.model.ThemeMode
import com.catlytics.core.widget.NowPlayingWidgetSync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var nowPlayingWidgetSync: NowPlayingWidgetSync

    @Inject
    lateinit var appPreferencesRepository: AppPreferencesRepository

    private val deepLinkFlow = MutableStateFlow<Uri?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        deepLinkFlow.value = intent?.data
        nowPlayingWidgetSync.start()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val deepLinkUri by deepLinkFlow.collectAsStateWithLifecycle()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguageState by themeViewModel.appLanguage.collectAsStateWithLifecycle()
            val appLanguage = appLanguageState ?: AppLanguage.System
            val systemInDarkTheme = isSystemInDarkTheme()

            val appLocale = remember(appLanguage) { appLanguage.resolveLocale() }
            val localizedContext = remember(this, appLanguage, appLocale) {
                if (appLanguage == AppLanguage.System) this else withAppLocale(appLocale)
            }
            val hasSyncedPlatformLocale = remember { mutableStateOf(false) }

            LaunchedEffect(appLanguage) {
                Locale.setDefault(appLocale)
                // No se borra el idioma por app del sistema en la primera composición:
                // onResume lo sincroniza con la preferencia interna.
                if (hasSyncedPlatformLocale.value || appLanguage != AppLanguage.System) {
                    syncPlatformLocale(appLanguage)
                }
                hasSyncedPlatformLocale.value = true
            }

            val darkThemeEnabled = when (themeMode ?: ThemeMode.System) {
                ThemeMode.System -> systemInDarkTheme
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            SideEffect {
                val systemBarStyle = if (darkThemeEnabled) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        scrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT,
                    )
                }
                enableEdgeToEdge(
                    statusBarStyle = systemBarStyle,
                    navigationBarStyle = systemBarStyle,
                )
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
            ) {
                CatlyticsTheme(darkTheme = darkThemeEnabled) {
                    CatlyticsApp(
                        deepLinkUri = deepLinkUri,
                        onDeepLinkHandled = { deepLinkFlow.value = null },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                val platformLanguage = platformAppLanguage()
                // Un valor vacío significa "sin preferencia del sistema": manda la app.
                if (platformLanguage == AppLanguage.System) return@launch
                val currentLanguage = appPreferencesRepository.observeAppLanguage().first()
                if (platformLanguage != currentLanguage) {
                    appPreferencesRepository.setAppLanguage(platformLanguage)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkFlow.value = intent.data
    }

    private fun syncPlatformLocale(language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val localeManager = getSystemService(LocaleManager::class.java) ?: return
        val desired = language.languageTag
            ?.let { LocaleList.forLanguageTags(it) }
            ?: LocaleList.getEmptyLocaleList()
        if (localeManager.applicationLocales != desired) {
            localeManager.applicationLocales = desired
        }
    }

    private fun platformAppLanguage(): AppLanguage {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return AppLanguage.System
        val localeManager = getSystemService(LocaleManager::class.java) ?: return AppLanguage.System
        val locales = localeManager.applicationLocales
        if (locales.isEmpty) return AppLanguage.System
        val locale = locales[0]
        return when (locale.language) {
            "es" -> AppLanguage.Spanish
            "en" -> AppLanguage.English
            else -> AppLanguage.System
        }
    }
}
