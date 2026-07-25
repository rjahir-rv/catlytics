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
import com.catlytics.core.domain.usecase.statistics.ExportStatisticsBackupUseCase
import com.catlytics.core.domain.usecase.statistics.ImportStatisticsBackupUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveStatisticsBackupSummaryUseCase
import com.catlytics.core.domain.usecase.statistics.PreviewStatisticsBackupUseCase
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.StatisticsBackupPreview
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.ThemeMode
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
    observeStatisticsBackupSummaryUseCase: ObserveStatisticsBackupSummaryUseCase,
    private val exportStatisticsBackupUseCase: ExportStatisticsBackupUseCase,
    private val previewStatisticsBackupUseCase: PreviewStatisticsBackupUseCase,
    private val importStatisticsBackupUseCase: ImportStatisticsBackupUseCase,
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

    val statisticsBackupSummary: StateFlow<StatisticsBackupSummary> =
        observeStatisticsBackupSummaryUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StatisticsBackupSummary(0, null, null),
            )

    private val _statisticsBackupStatus =
        MutableStateFlow<StatisticsBackupStatus>(StatisticsBackupStatus.Idle)
    val statisticsBackupStatus: StateFlow<StatisticsBackupStatus> =
        _statisticsBackupStatus.asStateFlow()

    private val _importPreview = MutableStateFlow<StatisticsBackupPreview?>(null)
    val importPreview: StateFlow<StatisticsBackupPreview?> = _importPreview.asStateFlow()

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

    fun exportStatisticsBackup(uri: String, appVersion: String) {
        if (isBackupBusy()) return
        backupJob = viewModelScope.launch {
            _statisticsBackupStatus.value = StatisticsBackupStatus.Exporting
            _statisticsBackupStatus.value = exportStatisticsBackupUseCase(uri, appVersion)
                .fold(
                    onSuccess = { StatisticsBackupStatus.ExportSuccess(it.eventCount) },
                    onFailure = { error ->
                        StatisticsBackupStatus.Error(
                            error.message ?: "No se pudo exportar el respaldo.",
                        )
                    },
                )
        }
    }

    fun loadImportPreview(uri: String) {
        if (isBackupBusy()) return
        pendingImportUri = uri
        backupJob = viewModelScope.launch {
            _statisticsBackupStatus.value = StatisticsBackupStatus.LoadingPreview
            previewStatisticsBackupUseCase(uri)
                .fold(
                    onSuccess = { preview ->
                        _importPreview.value = preview
                        _statisticsBackupStatus.value = StatisticsBackupStatus.Idle
                    },
                    onFailure = { error ->
                        pendingImportUri = null
                        _importPreview.value = null
                        _statisticsBackupStatus.value = StatisticsBackupStatus.Error(
                            error.message ?: "No se pudo leer el archivo de respaldo.",
                        )
                    },
                )
        }
    }

    fun confirmImport(mode: StatisticsImportMode) {
        val uri = pendingImportUri ?: return
        if (isBackupBusy()) return
        _importPreview.value = null
        backupJob = viewModelScope.launch {
            _statisticsBackupStatus.value = StatisticsBackupStatus.Importing
            _statisticsBackupStatus.value = importStatisticsBackupUseCase(uri, mode)
                .fold(
                    onSuccess = { result ->
                        pendingImportUri = null
                        StatisticsBackupStatus.ImportSuccess(
                            importedCount = result.importedCount,
                            skippedDuplicateCount = result.skippedDuplicateCount,
                            totalInFile = result.totalInFile,
                        )
                    },
                    onFailure = { error ->
                        StatisticsBackupStatus.Error(
                            error.message ?: "No se pudo importar el respaldo.",
                        )
                    },
                )
        }
    }

    fun dismissImportPreview() {
        pendingImportUri = null
        _importPreview.value = null
    }

    fun dismissStatisticsBackupStatus() {
        _statisticsBackupStatus.value = StatisticsBackupStatus.Idle
    }

    private fun isBackupBusy(): Boolean {
        val status = _statisticsBackupStatus.value
        return status is StatisticsBackupStatus.Exporting ||
            status is StatisticsBackupStatus.Importing ||
            status is StatisticsBackupStatus.LoadingPreview
    }
}

internal sealed interface MusicScanStatus {
    data object Idle : MusicScanStatus
    data object Scanning : MusicScanStatus
    data class Success(val newTrackCount: Int) : MusicScanStatus
    data class Error(val message: String) : MusicScanStatus
}

internal sealed interface StatisticsBackupStatus {
    data object Idle : StatisticsBackupStatus
    data object Exporting : StatisticsBackupStatus
    data object LoadingPreview : StatisticsBackupStatus
    data object Importing : StatisticsBackupStatus
    data class ExportSuccess(val eventCount: Int) : StatisticsBackupStatus
    data class ImportSuccess(
        val importedCount: Int,
        val skippedDuplicateCount: Int,
        val totalInFile: Int,
    ) : StatisticsBackupStatus
    data class Error(val message: String) : StatisticsBackupStatus
}
