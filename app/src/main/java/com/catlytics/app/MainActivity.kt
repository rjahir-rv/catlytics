package com.catlytics.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.app.ui.CatlyticsApp
import com.catlytics.app.ui.ThemeViewModel
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinkFlow = MutableStateFlow<Uri?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkFlow.value = intent?.data

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val deepLinkUri by deepLinkFlow.collectAsStateWithLifecycle()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            themeMode?.let { loadedThemeMode ->
                val darkThemeEnabled = when (loadedThemeMode) {
                    ThemeMode.System -> systemInDarkTheme
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }

                CatlyticsTheme(darkTheme = darkThemeEnabled) {
                    CatlyticsApp(
                        deepLinkUri = deepLinkUri,
                        onDeepLinkHandled = { deepLinkFlow.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkFlow.value = intent.data
    }
}
