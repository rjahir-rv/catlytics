package com.catlytics.feature.settings.impl

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.EqualizerRepository
import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.DEFAULT_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.domain.repository.SleepTimerController
import com.catlytics.core.domain.usecase.backup.ExportUnifiedBackupUseCase
import com.catlytics.core.domain.usecase.backup.ImportUnifiedBackupUseCase
import com.catlytics.core.domain.usecase.backup.ObserveUnifiedBackupSummaryUseCase
import com.catlytics.core.domain.usecase.backup.PreviewUnifiedBackupUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryFoldersUseCase
import com.catlytics.core.domain.usecase.library.ObserveMusicScanSettingsUseCase
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import com.catlytics.core.domain.usecase.library.SetFolderVisibilityUseCase
import com.catlytics.core.domain.usecase.library.SetMusicScanDurationFilterUseCase
import com.catlytics.core.domain.usecase.library.SetMusicScanSizeFilterUseCase
import com.catlytics.core.model.AppLanguage
import com.catlytics.core.model.BackupException
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
import com.catlytics.core.model.HomeRecommendationsSettings
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.ThemeMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.core.model.UnifiedExportResult
import com.catlytics.core.model.UnifiedImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val equalizerRepository: EqualizerRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val homePreferencesRepository: HomePreferencesRepository,
    private val sleepTimerController: SleepTimerController,
    observeLibraryFoldersUseCase: ObserveLibraryFoldersUseCase,
    observeMusicScanSettingsUseCase: ObserveMusicScanSettingsUseCase,
    private val refreshLibraryUseCase: RefreshLibraryUseCase,
    private val setFolderVisibilityUseCase: SetFolderVisibilityUseCase,
    private val setMusicScanDurationFilterUseCase: SetMusicScanDurationFilterUseCase,
    private val setMusicScanSizeFilterUseCase: SetMusicScanSizeFilterUseCase,
    observeUnifiedBackupSummaryUseCase: ObserveUnifiedBackupSummaryUseCase,
    private val exportUnifiedBackupUseCase: ExportUnifiedBackupUseCase,
    private val previewUnifiedBackupUseCase: PreviewUnifiedBackupUseCase,
    private val importUnifiedBackupUseCase: ImportUnifiedBackupUseCase,
) : ViewModel() {
    private val _musicScanStatus = MutableStateFlow<MusicScanStatus>(MusicScanStatus.Idle)
    private var scanSettingsUpdateJob: Job? = null
    private val _unifiedBackupStatus =
        MutableStateFlow<UnifiedBackupStatus>(UnifiedBackupStatus.Idle)
    private val _unifiedImportPreview = MutableStateFlow<UnifiedBackupPreview?>(null)
    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val preferences = combine(
        appPreferencesRepository.observeThemeMode(),
        appPreferencesRepository.observeAppLanguage(),
        homePreferencesRepository.observeHomeRecommendationsSettings(),
        equalizerRepository.observeEqualizerState(),
        playbackPreferencesRepository.observeCrossfadeDurationSeconds(),
    ) { themeMode, appLanguage, homeRecommendations, equalizer, crossfadeSeconds ->
        SettingsUiState(
            themeMode = themeMode,
            appLanguage = appLanguage,
            homeRecommendationsSettings = homeRecommendations,
            equalizerState = equalizer,
            crossfadeDurationSeconds = crossfadeSeconds,
        )
    }

    private val library = combine(
        observeLibraryFoldersUseCase(),
        observeMusicScanSettingsUseCase(),
        _musicScanStatus,
        ::Triple,
    )

    private val backup = combine(
        observeUnifiedBackupSummaryUseCase(),
        _unifiedBackupStatus,
        _unifiedImportPreview,
        ::Triple,
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences,
        sleepTimerController.state,
        library,
        backup,
    ) { preferences, sleepTimer, (folders, scanSettings, scanStatus), backup ->
        val (summary, backupStatus, preview) = backup
        preferences.copy(
            sleepTimerState = sleepTimer,
            libraryFolders = folders,
            musicScanSettings = scanSettings,
            musicScanStatus = scanStatus,
            unifiedBackupSummary = summary,
            unifiedBackupStatus = backupStatus,
            unifiedImportPreview = preview,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    private var pendingImportUri: String? = null
    private var backupJob: Job? = null

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            appPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appPreferencesRepository.setAppLanguage(language)
        }
    }

    fun setCrossfadeDurationSeconds(seconds: Int) {
        viewModelScope.launch {
            playbackPreferencesRepository.setCrossfadeDurationSeconds(seconds)
        }
    }

    fun setShowRecommendedPlaylists(show: Boolean) {
        viewModelScope.launch {
            homePreferencesRepository.setShowRecommendedPlaylists(show)
        }
    }

    fun setRecentAddedWindow(window: RecentAddedWindow) {
        viewModelScope.launch {
            homePreferencesRepository.setRecentAddedWindow(window)
        }
    }

    fun setShowNewTrackBadge(show: Boolean) {
        viewModelScope.launch {
            homePreferencesRepository.setShowNewTrackBadge(show)
        }
    }

    fun startSleepTimer(durationMinutes: Int) {
        sleepTimerController.start(durationMinutes)
    }

    fun cancelSleepTimer() {
        sleepTimerController.cancel()
    }

    fun setFolderVisible(folderId: String, visible: Boolean) {
        viewModelScope.launch {
            setFolderVisibilityUseCase(folderId, visible)
        }
    }

    fun setMusicScanDurationFilter(filter: MusicScanDurationFilter) {
        _musicScanStatus.value = MusicScanStatus.Idle
        scanSettingsUpdateJob = viewModelScope.launch {
            setMusicScanDurationFilterUseCase(filter)
        }
    }

    fun setMusicScanSizeFilter(filter: MusicScanSizeFilter) {
        _musicScanStatus.value = MusicScanStatus.Idle
        scanSettingsUpdateJob = viewModelScope.launch {
            setMusicScanSizeFilterUseCase(filter)
        }
    }

    fun scanMusic() {
        if (_musicScanStatus.value == MusicScanStatus.Scanning) return

        _musicScanStatus.value = MusicScanStatus.Scanning
        viewModelScope.launch {
            _musicScanStatus.value = try {
                scanSettingsUpdateJob?.join()
                MusicScanStatus.Success(refreshLibraryUseCase())
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Throwable) {
                MusicScanStatus.Error(UiText.Resource(R.string.settings_error_scan_failed))
            }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            equalizerRepository.setEnabled(enabled)
        }
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) {
        viewModelScope.launch {
            equalizerRepository.selectPreset(preset)
        }
    }

    fun setEqualizerMode(mode: EqualizerMode) {
        viewModelScope.launch {
            equalizerRepository.setMode(mode)
        }
    }

    fun setCustomBandLevel(bandId: Short, level: Int, isFinal: Boolean) {
        viewModelScope.launch {
            if (isFinal) {
                equalizerRepository.setBandLevel(bandId, level)
            } else {
                equalizerRepository.setBandLevelTransient(bandId, level)
            }
        }
    }

    fun refreshEqualizer() {
        viewModelScope.launch {
            equalizerRepository.refreshCapabilities()
        }
    }

    fun exportUnifiedBackup(
        uri: String,
        options: BackupOptions,
        appVersion: String,
    ) {
        if (isBackupBusy()) return
        backupJob = viewModelScope.launch {
            _unifiedBackupStatus.value = UnifiedBackupStatus.Exporting
            exportUnifiedBackupUseCase(uri, options, appVersion)
                .onSuccess { result ->
                    _unifiedBackupStatus.value = UnifiedBackupStatus.Idle
                    _events.emit(SettingsEvent.BackupExported(result))
                }
                .onFailure { error ->
                    _unifiedBackupStatus.value = UnifiedBackupStatus.Error(
                        error.toBackupMessage(R.string.settings_error_backup_export_failed),
                    )
                }
        }
    }

    fun loadUnifiedImportPreview(uri: String) {
        if (isBackupBusy()) return
        pendingImportUri = uri
        backupJob = viewModelScope.launch {
            _unifiedBackupStatus.value = UnifiedBackupStatus.LoadingPreview
            previewUnifiedBackupUseCase(uri)
                .fold(
                    onSuccess = { preview ->
                        _unifiedImportPreview.value = preview
                        _unifiedBackupStatus.value = UnifiedBackupStatus.Idle
                    },
                    onFailure = { error ->
                        pendingImportUri = null
                        _unifiedImportPreview.value = null
                        _unifiedBackupStatus.value = UnifiedBackupStatus.Error(
                            error.toBackupMessage(R.string.settings_error_backup_read_failed),
                        )
                    },
                )
        }
    }

    fun confirmUnifiedImport(
        options: BackupOptions,
        mode: StatisticsImportMode,
    ) {
        val uri = pendingImportUri ?: return
        if (isBackupBusy()) return
        _unifiedImportPreview.value = null
        backupJob = viewModelScope.launch {
            _unifiedBackupStatus.value = UnifiedBackupStatus.Importing
            _unifiedBackupStatus.value = importUnifiedBackupUseCase(uri, options, mode)
                .fold(
                    onSuccess = { result ->
                        pendingImportUri = null
                        UnifiedBackupStatus.ImportSuccess(result)
                    },
                    onFailure = { error ->
                        UnifiedBackupStatus.Error(
                            error.toBackupMessage(R.string.settings_error_backup_import_failed),
                        )
                    },
                )
        }
    }

    fun dismissUnifiedImportPreview() {
        pendingImportUri = null
        _unifiedImportPreview.value = null
    }

    fun dismissUnifiedBackupStatus() {
        _unifiedBackupStatus.value = UnifiedBackupStatus.Idle
    }

    private fun isBackupBusy(): Boolean {
        val status = _unifiedBackupStatus.value
        return status is UnifiedBackupStatus.Exporting ||
            status is UnifiedBackupStatus.Importing ||
            status is UnifiedBackupStatus.LoadingPreview
    }
}

private fun Throwable.toBackupMessage(@StringRes fallback: Int): UiText = when (this) {
    is BackupException.TooLarge -> UiText.Resource(R.string.settings_error_backup_too_large)
    is BackupException.InvalidFormat -> UiText.Resource(R.string.settings_error_backup_invalid_format)
    is BackupException.UnsupportedVersion -> UiText.Resource(
        R.string.settings_error_backup_unsupported_version,
        listOf(schemaVersion),
    )
    is BackupException.InvalidContent -> UiText.Resource(R.string.settings_error_backup_invalid_content)
    else -> UiText.Resource(fallback)
}

internal data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val appLanguage: AppLanguage = AppLanguage.System,
    val homeRecommendationsSettings: HomeRecommendationsSettings = HomeRecommendationsSettings(),
    val equalizerState: EqualizerState = EqualizerState(),
    val crossfadeDurationSeconds: Int = DEFAULT_CROSSFADE_DURATION_SECONDS,
    val sleepTimerState: SleepTimerState = SleepTimerState.Inactive,
    val libraryFolders: List<LibraryFolder> = emptyList(),
    val musicScanSettings: MusicScanSettings = MusicScanSettings(),
    val musicScanStatus: MusicScanStatus = MusicScanStatus.Idle,
    val unifiedBackupSummary: UnifiedBackupSummary = UnifiedBackupSummary(),
    val unifiedBackupStatus: UnifiedBackupStatus = UnifiedBackupStatus.Idle,
    val unifiedImportPreview: UnifiedBackupPreview? = null,
)

internal sealed interface SettingsEvent {
    data class BackupExported(val result: UnifiedExportResult) : SettingsEvent
}

internal sealed interface MusicScanStatus {
    data object Idle : MusicScanStatus
    data object Scanning : MusicScanStatus
    data class Success(val newTrackCount: Int) : MusicScanStatus
    data class Error(val message: UiText) : MusicScanStatus
}

internal sealed interface UnifiedBackupStatus {
    data object Idle : UnifiedBackupStatus
    data object Exporting : UnifiedBackupStatus
    data object LoadingPreview : UnifiedBackupStatus
    data object Importing : UnifiedBackupStatus
    data class ImportSuccess(val result: UnifiedImportResult) : UnifiedBackupStatus
    data class Error(val message: UiText) : UnifiedBackupStatus
}
