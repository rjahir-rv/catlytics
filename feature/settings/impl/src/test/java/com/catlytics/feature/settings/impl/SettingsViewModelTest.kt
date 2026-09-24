package com.catlytics.feature.settings.impl

import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.EqualizerRepository
import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository
import com.catlytics.core.domain.repository.SleepTimerController
import com.catlytics.core.domain.repository.UnifiedBackupRepository
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
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerState
import com.catlytics.core.model.HomeRecommendationsSettings
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.PlaylistBackupPreview
import com.catlytics.core.model.PlaylistExportResult
import com.catlytics.core.model.PlaylistImportResult
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.SortDirection
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.StatisticsBackupPreview
import com.catlytics.core.model.StatisticsExportResult
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.StatisticsImportResult
import com.catlytics.core.model.ThemeMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.core.model.UnifiedExportResult
import com.catlytics.core.model.UnifiedImportResult
import com.catlytics.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `setThemeMode persists every available mode`() = runTest {
        val repository = FakeAppPreferencesRepository()
        val equalizerRepository = FakeEqualizerRepository()
        val playbackPreferencesRepository = FakePlaybackPreferencesRepository()
        val viewModel = viewModel(repository, equalizerRepository, playbackPreferencesRepository)

        ThemeMode.entries.forEach { themeMode ->
            viewModel.setThemeMode(themeMode)
            advanceUntilIdle()

            assertEquals(themeMode, repository.themeMode.value)
            assertEquals(themeMode, viewModel.themeMode.value)
        }
    }

    @Test
    fun `setEqualizerEnabled persists requested state`() = runTest {
        val equalizerRepository = FakeEqualizerRepository()
        val viewModel = viewModel(
            FakeAppPreferencesRepository(),
            equalizerRepository,
            FakePlaybackPreferencesRepository(),
        )

        viewModel.setEqualizerEnabled(true)
        advanceUntilIdle()

        assertEquals(true, equalizerRepository.state.value.enabled)
        assertEquals(true, viewModel.equalizerState.value.enabled)
    }

    @Test
    fun `selectEqualizerPreset persists selected preset`() = runTest {
        val equalizerRepository = FakeEqualizerRepository()
        val viewModel = viewModel(
            FakeAppPreferencesRepository(),
            equalizerRepository,
            FakePlaybackPreferencesRepository(),
        )
        val preset = EqualizerPreset(id = 1, name = "Rock")

        viewModel.selectEqualizerPreset(preset)
        advanceUntilIdle()

        assertEquals("Rock", equalizerRepository.state.value.selectedPresetName)
        assertEquals("Rock", viewModel.equalizerState.value.selectedPresetName)
    }

    @Test
    fun `setCrossfadeDuration persists requested value`() = runTest {
        val playbackPreferencesRepository = FakePlaybackPreferencesRepository()
        val viewModel = viewModel(
            FakeAppPreferencesRepository(),
            FakeEqualizerRepository(),
            playbackPreferencesRepository,
        )

        viewModel.setCrossfadeDurationSeconds(7)
        advanceUntilIdle()

        assertEquals(7, playbackPreferencesRepository.durationSeconds.value)
        assertEquals(7, viewModel.crossfadeDurationSeconds.value)
    }

    @Test
    fun `home recommendation settings are persisted`() = runTest {
        val homePreferencesRepository = FakeSettingsHomePreferencesRepository()
        val viewModel = viewModel(homePreferencesRepository = homePreferencesRepository)

        viewModel.setShowRecommendedPlaylists(false)
        viewModel.setRecentAddedWindow(RecentAddedWindow.Days14)
        viewModel.setShowNewTrackBadge(false)
        advanceUntilIdle()

        val expected = HomeRecommendationsSettings(
            showRecommendedPlaylists = false,
            recentAddedWindow = RecentAddedWindow.Days14,
            showNewTrackBadge = false,
        )
        assertEquals(expected, homePreferencesRepository.settings.value)
        assertEquals(expected, viewModel.homeRecommendationsSettings.value)
    }

    @Test
    fun `sleep timer can be started and cancelled`() = runTest {
        val sleepTimerController = FakeSleepTimerController()
        val viewModel = viewModel(sleepTimerController = sleepTimerController)

        viewModel.startSleepTimer(durationMinutes = 45)

        assertEquals(listOf(45), sleepTimerController.startedDurations)
        assertEquals(
            SleepTimerState.Active(2_700_000L, 2_700_000L),
            viewModel.sleepTimerState.value,
        )

        viewModel.cancelSleepTimer()

        assertEquals(1, sleepTimerController.cancelCalls)
        assertEquals(SleepTimerState.Inactive, viewModel.sleepTimerState.value)
    }

    @Test
    fun `scan filters persist and successful scan exposes count`() = runTest {
        val libraryRepository = FakeLibraryRepository(refreshResult = 23)
        val libraryPreferencesRepository = FakeLibraryPreferencesRepository()
        val viewModel = viewModel(
            libraryRepository = libraryRepository,
            libraryPreferencesRepository = libraryPreferencesRepository,
        )

        viewModel.setMusicScanDurationFilter(MusicScanDurationFilter.Seconds30)
        viewModel.setMusicScanSizeFilter(MusicScanSizeFilter.Megabyte1)
        advanceUntilIdle()
        viewModel.scanMusic()
        advanceUntilIdle()

        assertEquals(
            MusicScanSettings(
                durationFilter = MusicScanDurationFilter.Seconds30,
                sizeFilter = MusicScanSizeFilter.Megabyte1,
            ),
            libraryPreferencesRepository.settings.value,
        )
        assertEquals(MusicScanStatus.Success(23), viewModel.musicScanStatus.value)
        assertEquals(1, libraryRepository.refreshCalls)
    }

    @Test
    fun `scan failure is exposed to the screen`() = runTest {
        val libraryRepository = FakeLibraryRepository(refreshError = IllegalStateException("Falló MediaStore"))
        val viewModel = viewModel(libraryRepository = libraryRepository)

        viewModel.scanMusic()
        advanceUntilIdle()

        assertEquals(MusicScanStatus.Error("Falló MediaStore"), viewModel.musicScanStatus.value)
    }

    @Test
    fun `exportUnifiedBackup updates status on success`() = runTest {
        val viewModel = viewModel()
        viewModel.exportUnifiedBackup("content://export", BackupOptions(), "0.0.5")
        advanceUntilIdle()

        val status = viewModel.unifiedBackupStatus.value
        assertEquals(true, status is UnifiedBackupStatus.ExportSuccess)
    }

    @Test
    fun `loadUnifiedImportPreview updates preview state`() = runTest {
        val viewModel = viewModel()
        viewModel.loadUnifiedImportPreview("content://import")
        advanceUntilIdle()

        assertEquals(0, viewModel.unifiedImportPreview.value?.playlists?.playlistCount)
    }

    @Test
    fun `confirmUnifiedImport completes and updates status`() = runTest {
        val viewModel = viewModel()
        viewModel.loadUnifiedImportPreview("content://import")
        advanceUntilIdle()

        viewModel.confirmUnifiedImport(BackupOptions(), StatisticsImportMode.Merge)
        advanceUntilIdle()

        val status = viewModel.unifiedBackupStatus.value
        assertEquals(true, status is UnifiedBackupStatus.ImportSuccess)
    }

    private fun viewModel(
        appPreferencesRepository: AppPreferencesRepository = FakeAppPreferencesRepository(),
        equalizerRepository: EqualizerRepository = FakeEqualizerRepository(),
        playbackPreferencesRepository: PlaybackPreferencesRepository = FakePlaybackPreferencesRepository(),
        homePreferencesRepository: HomePreferencesRepository = FakeSettingsHomePreferencesRepository(),
        sleepTimerController: SleepTimerController = FakeSleepTimerController(),
        libraryRepository: FakeLibraryRepository = FakeLibraryRepository(),
        libraryPreferencesRepository: FakeLibraryPreferencesRepository =
            FakeLibraryPreferencesRepository(),
        unifiedBackupRepository: UnifiedBackupRepository = FakeUnifiedBackupRepository(),
    ) = SettingsViewModel(
        appPreferencesRepository = appPreferencesRepository,
        equalizerRepository = equalizerRepository,
        playbackPreferencesRepository = playbackPreferencesRepository,
        homePreferencesRepository = homePreferencesRepository,
        sleepTimerController = sleepTimerController,
        observeLibraryFoldersUseCase = ObserveLibraryFoldersUseCase(libraryRepository),
        observeMusicScanSettingsUseCase =
            ObserveMusicScanSettingsUseCase(libraryPreferencesRepository),
        refreshLibraryUseCase = RefreshLibraryUseCase(libraryRepository),
        setFolderVisibilityUseCase = SetFolderVisibilityUseCase(libraryRepository),
        setMusicScanDurationFilterUseCase =
            SetMusicScanDurationFilterUseCase(libraryPreferencesRepository),
        setMusicScanSizeFilterUseCase =
            SetMusicScanSizeFilterUseCase(libraryPreferencesRepository),
        observeUnifiedBackupSummaryUseCase =
            ObserveUnifiedBackupSummaryUseCase(unifiedBackupRepository),
        exportUnifiedBackupUseCase = ExportUnifiedBackupUseCase(unifiedBackupRepository),
        previewUnifiedBackupUseCase = PreviewUnifiedBackupUseCase(unifiedBackupRepository),
        importUnifiedBackupUseCase = ImportUnifiedBackupUseCase(unifiedBackupRepository),
    )
}

private class FakeUnifiedBackupRepository : UnifiedBackupRepository {
    override fun observeSummary(): Flow<UnifiedBackupSummary> =
        flowOf(UnifiedBackupSummary())

    override suspend fun exportToUri(
        uri: String,
        options: BackupOptions,
        appVersion: String,
    ): Result<UnifiedExportResult> = Result.success(
        UnifiedExportResult(
            statistics = StatisticsExportResult(0),
            playlists = PlaylistExportResult(0, 0),
        ),
    )

    override suspend fun previewFromUri(uri: String): Result<UnifiedBackupPreview> =
        Result.success(
            UnifiedBackupPreview(
                schemaVersion = 3,
                exportedAtMillis = 0L,
                statistics = StatisticsBackupPreview(
                    schemaVersion = 3,
                    exportedAtMillis = 0L,
                    eventCount = 0,
                    firstEventMillis = null,
                    lastEventMillis = null,
                ),
                playlists = PlaylistBackupPreview(
                    schemaVersion = 3,
                    exportedAtMillis = 0L,
                    playlistCount = 0,
                    totalTracksInBackup = 0,
                    matchedTracksCount = 0,
                    missingTracksCount = 0,
                    likedTracksCount = 0,
                ),
            ),
        )

    override suspend fun importFromUri(
        uri: String,
        options: BackupOptions,
        mode: StatisticsImportMode,
    ): Result<UnifiedImportResult> = Result.success(
        UnifiedImportResult(
            statistics = StatisticsImportResult(0, 0, 0),
            playlists = PlaylistImportResult(0, 0, 0),
        ),
    )
}

private class FakeLibraryRepository(
    private val refreshResult: Int = 0,
    private val refreshError: Throwable? = null,
) : LibraryRepository {
    var refreshCalls = 0
        private set

    override fun observeAlbums() = flowOf(emptyList<Album>())
    override fun observeAlbumContent(albumId: String) = flowOf<AlbumContent?>(null)
    override fun observeArtists() = flowOf(emptyList<ArtistSummary>())
    override fun observeArtistContent(artistId: String) = flowOf<ArtistContent?>(null)
    override fun observeTracks() = flowOf(emptyList<Track>())
    override fun observeAllTracks() = flowOf(emptyList<Track>())
    override fun observeFolders() = flowOf(emptyList<LibraryFolder>())
    override fun observeFolderContent(folderId: String) = flowOf<LibraryFolderContent?>(null)
    override suspend fun resolvePlaylistSource(source: PlaylistSource) = emptyList<Track>()
    override suspend fun refreshTracks(): Int {
        refreshCalls++
        refreshError?.let { throw it }
        return refreshResult
    }
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

private class FakeLibraryPreferencesRepository : LibraryPreferencesRepository {
    val settings = MutableStateFlow(MusicScanSettings())

    override fun observeHiddenFolderIds() = flowOf(emptySet<String>())
    override fun observeMusicScanSettings() = settings
    override fun observeArtistViewMode() = flowOf(com.catlytics.core.model.ArtistViewMode.List)
    override fun observePlaylistViewMode() = flowOf(PlaylistViewMode.List)
    override fun observeLibrarySortDirection() = flowOf(SortDirection.Ascending)
    override fun observePlaylistSortDirection() = flowOf(SortDirection.Ascending)
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
    override suspend fun setMusicScanDurationFilter(filter: MusicScanDurationFilter) {
        settings.value = settings.value.copy(durationFilter = filter)
    }
    override suspend fun setMusicScanSizeFilter(filter: MusicScanSizeFilter) {
        settings.value = settings.value.copy(sizeFilter = filter)
    }
    override suspend fun setArtistViewMode(viewMode: com.catlytics.core.model.ArtistViewMode) = Unit
    override suspend fun setPlaylistViewMode(viewMode: PlaylistViewMode) = Unit
    override suspend fun setLibrarySortDirection(direction: SortDirection) = Unit
    override suspend fun setPlaylistSortDirection(direction: SortDirection) = Unit
}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    val themeMode = MutableStateFlow(ThemeMode.System)

    override fun observeThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        this.themeMode.value = themeMode
    }
}

private class FakeEqualizerRepository : EqualizerRepository {
    val state = MutableStateFlow(
        EqualizerState(
            isAvailable = true,
            presets = listOf(
                EqualizerPreset(id = 0, name = "Normal"),
                EqualizerPreset(id = 1, name = "Rock"),
            ),
        ),
    )

    override fun observeEqualizerState(): Flow<EqualizerState> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled)
    }

    override suspend fun selectPreset(preset: EqualizerPreset) {
        state.value = state.value.copy(selectedPresetName = preset.name)
    }

    override suspend fun setMode(mode: EqualizerMode) {
        state.value = state.value.copy(mode = mode)
    }

    override suspend fun setBandLevel(bandId: Short, level: Int) {
        // Fake implementation
    }

    override suspend fun setBandLevelTransient(bandId: Short, level: Int) {}

    override suspend fun refreshCapabilities() = Unit
}

private class FakePlaybackPreferencesRepository : PlaybackPreferencesRepository {
    val durationSeconds = MutableStateFlow(
        PlaybackPreferencesRepository.DEFAULT_CROSSFADE_DURATION_SECONDS,
    )

    override fun observeCrossfadeDurationSeconds(): Flow<Int> = durationSeconds

    override suspend fun setCrossfadeDurationSeconds(seconds: Int) {
        durationSeconds.value = seconds
    }
}

private class FakeSettingsHomePreferencesRepository : HomePreferencesRepository {
    val settings = MutableStateFlow(HomeRecommendationsSettings())

    override fun observeHomeRecommendationsSettings(): Flow<HomeRecommendationsSettings> = settings

    override suspend fun setShowRecommendedPlaylists(show: Boolean) {
        settings.value = settings.value.copy(showRecommendedPlaylists = show)
    }

    override suspend fun setRecentAddedWindow(window: RecentAddedWindow) {
        settings.value = settings.value.copy(recentAddedWindow = window)
    }

    override suspend fun setShowNewTrackBadge(show: Boolean) {
        settings.value = settings.value.copy(showNewTrackBadge = show)
    }
}

private class FakeSleepTimerController : SleepTimerController {
    private val mutableState = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)
    override val state = mutableState
    val startedDurations = mutableListOf<Int>()
    var cancelCalls = 0

    override fun start(durationMinutes: Int) {
        startedDurations += durationMinutes
        val durationMillis = durationMinutes * 60_000L
        mutableState.value = SleepTimerState.Active(durationMillis, durationMillis)
    }

    override fun cancel() {
        cancelCalls += 1
        mutableState.value = SleepTimerState.Inactive
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
