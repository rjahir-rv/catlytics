package com.catlytics.feature.settings.impl

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.BackupOptions
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun SettingsRoute(
    appVersion: String,
    hasAudioPermission: Boolean = true,
    onRequestAudioPermission: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {},
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val libraryFolders by viewModel.libraryFolders.collectAsStateWithLifecycle()
    val musicScanSettings by viewModel.musicScanSettings.collectAsStateWithLifecycle()
    val musicScanStatus by viewModel.musicScanStatus.collectAsStateWithLifecycle()
    val unifiedBackupSummary by viewModel.unifiedBackupSummary.collectAsStateWithLifecycle()
    val unifiedBackupStatus by viewModel.unifiedBackupStatus.collectAsStateWithLifecycle()
    val unifiedImportPreview by viewModel.unifiedImportPreview.collectAsStateWithLifecycle()

    var pendingExportOptions by remember { mutableStateOf<BackupOptions?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val options = pendingExportOptions
        if (uri != null && options != null) {
            viewModel.exportUnifiedBackup(uri.toString(), options, appVersion)
        }
        pendingExportOptions = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.loadUnifiedImportPreview(uri.toString())
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshEqualizer()
    }

    LaunchedEffect(unifiedBackupStatus) {
        when (val status = unifiedBackupStatus) {
            is UnifiedBackupStatus.ExportSuccess -> {
                val stats = status.result.statistics
                val playlists = status.result.playlists
                val parts = mutableListOf<String>()
                if (playlists != null) parts.add("${playlists.playlistCount} playlists")
                if (stats != null) parts.add("${stats.eventCount} reproducciones")
                val detail = if (parts.isNotEmpty()) " (${parts.joinToString(", ")})" else ""
                Toast.makeText(
                    context,
                    "Copia de seguridad exportada con éxito$detail",
                    Toast.LENGTH_SHORT,
                ).show()
                viewModel.dismissUnifiedBackupStatus()
            }
            else -> Unit
        }
    }

    SettingsScreen(
        appVersion = appVersion,
        themeMode = themeMode,
        equalizerState = equalizerState,
        crossfadeDurationSeconds = crossfadeDurationSeconds,
        sleepTimerState = sleepTimerState,
        libraryFolders = libraryFolders,
        musicScanSettings = musicScanSettings,
        musicScanStatus = musicScanStatus,
        hasAudioPermission = hasAudioPermission,
        unifiedBackupSummary = unifiedBackupSummary,
        unifiedBackupStatus = unifiedBackupStatus,
        unifiedImportPreview = unifiedImportPreview,
        onRequestAudioPermission = onRequestAudioPermission,
        onThemeModeChange = viewModel::setThemeMode,
        onCrossfadeDurationChange = viewModel::setCrossfadeDurationSeconds,
        onSleepTimerStart = viewModel::startSleepTimer,
        onSleepTimerCancel = viewModel::cancelSleepTimer,
        onFolderVisibilityChange = viewModel::setFolderVisible,
        onMusicScanDurationFilterChange = viewModel::setMusicScanDurationFilter,
        onMusicScanSizeFilterChange = viewModel::setMusicScanSizeFilter,
        onScanMusic = viewModel::scanMusic,
        onEqualizerEnabledChange = viewModel::setEqualizerEnabled,
        onEqualizerModeChange = viewModel::setEqualizerMode,
        onEqualizerPresetSelected = viewModel::selectEqualizerPreset,
        onCustomBandLevelChange = viewModel::setCustomBandLevel,
        onExportBackupClick = { options ->
            pendingExportOptions = options
            val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            exportLauncher.launch("catlytics-backup-$date.json")
        },
        onImportBackupClick = {
            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onConfirmImport = viewModel::confirmUnifiedImport,
        onDismissImportPreview = viewModel::dismissUnifiedImportPreview,
        onDismissBackupStatus = viewModel::dismissUnifiedBackupStatus,
        bottomPadding = bottomPadding,
        onTopBarTitleChange = onTopBarTitleChange,
        onTopBarBackActionChange = onTopBarBackActionChange,
        scaffoldContentPadding = scaffoldContentPadding,
    )
}
