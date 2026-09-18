package com.catlytics.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.EqualizerRepository
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository
import com.catlytics.core.domain.repository.SleepTimerController
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.DEFAULT_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.domain.usecase.library.ObserveLibraryFoldersUseCase
import com.catlytics.core.domain.usecase.library.ObserveMusicScanSettingsUseCase
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import com.catlytics.core.domain.usecase.library.SetFolderVisibilityUseCase
import com.catlytics.core.domain.usecase.library.SetMusicScanDurationFilterUseCase
import com.catlytics.core.domain.usecase.library.SetMusicScanSizeFilterUseCase
import com.catlytics.core.domain.usecase.backup.ExportUnifiedBackupUseCase
import com.catlytics.core.domain.usecase.backup.ImportUnifiedBackupUseCase
import com.catlytics.core.domain.usecase.backup.ObserveUnifiedBackupSummaryUseCase
import com.catlytics.core.domain.usecase.backup.PreviewUnifiedBackupUseCase
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.ThemeMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.core.model.UnifiedExportResult
import com.catlytics.core.model.UnifiedImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val equalizerRepository: EqualizerRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
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
    val sleepTimerState = sleepTimerController.state

    val themeMode: StateFlow<ThemeMode> = appPreferencesRepository.observeThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.System,
        )
    val equalizerState: StateFlow<EqualizerState> = equalizerRepository.observeEqualizerState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EqualizerState(),
        )
    val crossfadeDurationSeconds: StateFlow<Int> =
        playbackPreferencesRepository.observeCrossfadeDurationSeconds()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DEFAULT_CROSSFADE_DURATION_SECONDS,
            )
    val libraryFolders: StateFlow<List<LibraryFolder>> = observeLibraryFoldersUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
    val musicScanSettings: StateFlow<MusicScanSettings> = observeMusicScanSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MusicScanSettings(),
        )

    private val _musicScanStatus = MutableStateFlow<MusicScanStatus>(MusicScanStatus.Idle)
    val musicScanStatus: StateFlow<MusicScanStatus> = _musicScanStatus.asStateFlow()
    private var scanSettingsUpdateJob: Job? = null

    val unifiedBackupSummary: StateFlow<UnifiedBackupSummary> =
        observeUnifiedBackupSummaryUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = UnifiedBackupSummary(),
            )

    private val _unifiedBackupStatus =
        MutableStateFlow<UnifiedBackupStatus>(UnifiedBackupStatus.Idle)
    val unifiedBackupStatus: StateFlow<UnifiedBackupStatus> =
        _unifiedBackupStatus.asStateFlow()

    private val _unifiedImportPreview = MutableStateFlow<UnifiedBackupPreview?>(null)
    val unifiedImportPreview: StateFlow<UnifiedBackupPreview?> = _unifiedImportPreview.asStateFlow()

    private var pendingImportUri: String? = null
    private var backupJob: Job? = null

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            appPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setCrossfadeDurationSeconds(seconds: Int) {
        viewModelScope.launch {
            playbackPreferencesRepository.setCrossfadeDurationSeconds(seconds)
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
            } catch (throwable: Throwable) {
                MusicScanStatus.Error(
                    throwable.message ?: "No se pudo escanear la música del dispositivo.",
                )
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
            _unifiedBackupStatus.value = exportUnifiedBackupUseCase(uri, options, appVersion)
                .fold(
                    onSuccess = { result ->
                        UnifiedBackupStatus.ExportSuccess(result)
                    },
                    onFailure = { error ->
                        UnifiedBackupStatus.Error(
                            error.message ?: "No se pudo exportar la copia de seguridad.",
                        )
                    },
                )
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
                            error.message ?: "No se pudo leer el archivo de respaldo.",
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
                            error.message ?: "No se pudo importar la copia de seguridad.",
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

internal sealed interface MusicScanStatus {
    data object Idle : MusicScanStatus
    data object Scanning : MusicScanStatus
    data class Success(val newTrackCount: Int) : MusicScanStatus
    data class Error(val message: String) : MusicScanStatus
}

internal sealed interface UnifiedBackupStatus {
    data object Idle : UnifiedBackupStatus
    data object Exporting : UnifiedBackupStatus
    data object LoadingPreview : UnifiedBackupStatus
    data object Importing : UnifiedBackupStatus
    data class ExportSuccess(val result: UnifiedExportResult) : UnifiedBackupStatus
    data class ImportSuccess(val result: UnifiedImportResult) : UnifiedBackupStatus
    data class Error(val message: String) : UnifiedBackupStatus
}

