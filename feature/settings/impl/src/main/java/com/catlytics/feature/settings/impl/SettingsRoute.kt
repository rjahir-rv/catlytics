package com.catlytics.feature.settings.impl

import android.content.res.Resources
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.R as DesignSystemR
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.UnifiedExportResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun SettingsRoute(
    appVersion: String,
    @RawRes librariesRes: Int,
    hasAudioPermission: Boolean = true,
    onRequestAudioPermission: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    onTopBarTitleChange: (String) -> Unit = {},
    onTopBarBackActionChange: ((() -> Unit)?) -> Unit = {},
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Sobrevive a que el sistema recree la actividad mientras el selector de archivos está abierto.
    var pendingExportOptions by rememberSaveable(stateSaver = BackupOptionsSaver) {
        mutableStateOf<BackupOptions?>(null)
    }

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
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.refreshEqualizer()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.BackupExported -> Toast.makeText(
                    context,
                    resources.backupExportedMessage(event.result),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    SettingsScreen(
        appVersion = appVersion,
        librariesRes = librariesRes,
        themeMode = uiState.themeMode,
        appLanguage = uiState.appLanguage,
        homeRecommendationsSettings = uiState.homeRecommendationsSettings,
        equalizerState = uiState.equalizerState,
        crossfadeDurationSeconds = uiState.crossfadeDurationSeconds,
        sleepTimerState = uiState.sleepTimerState,
        libraryFolders = uiState.libraryFolders,
        musicScanSettings = uiState.musicScanSettings,
        musicScanStatus = uiState.musicScanStatus,
        hasAudioPermission = hasAudioPermission,
        unifiedBackupSummary = uiState.unifiedBackupSummary,
        unifiedBackupStatus = uiState.unifiedBackupStatus,
        unifiedImportPreview = uiState.unifiedImportPreview,
        onRequestAudioPermission = onRequestAudioPermission,
        onThemeModeChange = viewModel::setThemeMode,
        onAppLanguageChange = viewModel::setAppLanguage,
        onShowRecommendedPlaylistsChange = viewModel::setShowRecommendedPlaylists,
        onRecentAddedWindowChange = viewModel::setRecentAddedWindow,
        onShowNewTrackBadgeChange = viewModel::setShowNewTrackBadge,
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

private val BackupOptionsSaver = Saver<BackupOptions?, List<Boolean>>(
    save = { options -> options?.let { listOf(it.includeStatistics, it.includePlaylists) } },
    restore = { (includeStatistics, includePlaylists) ->
        BackupOptions(includeStatistics, includePlaylists)
    },
)

private fun Resources.backupExportedMessage(result: UnifiedExportResult): String {
    val parts = buildList {
        result.playlists?.let { playlists ->
            add(
                getQuantityString(
                    R.plurals.settings_backup_export_toast_playlists,
                    playlists.playlistCount,
                    playlists.playlistCount,
                ),
            )
        }
        result.statistics?.let { stats ->
            add(
                getQuantityString(
                    DesignSystemR.plurals.ds_play_count,
                    stats.eventCount,
                    stats.eventCount,
                ),
            )
        }
    }
    val detail = if (parts.isNotEmpty()) {
        getString(R.string.settings_backup_export_toast_detail, parts.joinToString(", "))
    } else {
        ""
    }
    return getString(R.string.settings_backup_export_toast_success) + detail
}
