package com.catlytics.feature.home.impl

import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.usecase.library.ObserveRecentlyAddedTracksUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.HomeRecommendationsSettings
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class RecentlyAddedViewModelTest {
    @get:Rule
    val mainDispatcherRule = RecentlyAddedMainDispatcherRule()

    private lateinit var repository: FakeRecentlyAddedLibraryRepository
    private lateinit var playbackController: FakeRecentlyAddedPlaybackController

    @Before
    fun setUp() {
        repository = FakeRecentlyAddedLibraryRepository()
        playbackController = FakeRecentlyAddedPlaybackController()
    }

    @Test
    fun `uiState is empty when there are no recently added tracks`() = runTest {
        repository.setTracks(listOf(track(id = "track-1", addedAtMillis = null)))
        val viewModel = recentlyAddedViewModel()
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(RecentlyAddedUiState.Empty(21), viewModel.uiState.value)
    }

    @Test
    fun `uiState is success with recent tracks newest first`() = runTest {
        val older = track(id = "track-1", addedAtMillis = NOW_MILLIS - 5 * DAY_MILLIS)
        val newer = track(id = "track-2", addedAtMillis = NOW_MILLIS - DAY_MILLIS)
        repository.setTracks(listOf(older, newer))
        val viewModel = recentlyAddedViewModel()
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(
            RecentlyAddedUiState.Success(tracks = listOf(newer, older)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `onTrackSelected plays selected track with recent queue`() = runTest {
        val older = track(id = "track-1", addedAtMillis = NOW_MILLIS - 2 * DAY_MILLIS)
        val newer = track(id = "track-2", addedAtMillis = NOW_MILLIS - DAY_MILLIS)
        repository.setTracks(listOf(older, newer))
        val viewModel = recentlyAddedViewModel()
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RecentlyAddedUiState.Success
        viewModel.onTrackSelected(state.tracks[1], state.tracks)
        advanceUntilIdle()

        assertEquals(state.tracks[1], playbackController.playedTrack)
        assertEquals(state.tracks, playbackController.playedQueue)
    }

    @Test
    fun `onPlayAll and onShuffle play the recent queue`() = runTest {
        val older = track(id = "track-1", addedAtMillis = NOW_MILLIS - 2 * DAY_MILLIS)
        val newer = track(id = "track-2", addedAtMillis = NOW_MILLIS - DAY_MILLIS)
        repository.setTracks(listOf(older, newer))
        val recentQueue = listOf(newer, older)
        val viewModel = recentlyAddedViewModel()
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        viewModel.onPlayAll()
        advanceUntilIdle()
        assertEquals(recentQueue.first(), playbackController.playedTrack)
        assertEquals(recentQueue, playbackController.playedQueue)

        viewModel.onShuffle()
        advanceUntilIdle()
        assertEquals(true, playbackController.shuffleEnabled)
        assertEquals(recentQueue.toSet(), playbackController.playedQueue.toSet())
    }

    @Test
    fun `uiState starts as loading`() = runTest {
        val viewModel = recentlyAddedViewModel()

        assertTrue(viewModel.uiState.value is RecentlyAddedUiState.Loading)
    }

    @Test
    fun `empty state exposes the configured window`() = runTest {
        val homePreferencesRepository = FakeRecentlyAddedHomePreferencesRepository(
            HomeRecommendationsSettings(recentAddedWindow = RecentAddedWindow.Days10),
        )
        val viewModel = recentlyAddedViewModel(homePreferencesRepository)
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(RecentlyAddedUiState.Empty(10), viewModel.uiState.value)
    }

    @Test
    fun `success state follows the window and new badge settings`() = runTest {
        val recent = track(id = "track-1", addedAtMillis = NOW_MILLIS - DAY_MILLIS)
        repository.setTracks(listOf(recent))
        val homePreferencesRepository = FakeRecentlyAddedHomePreferencesRepository(
            HomeRecommendationsSettings(
                recentAddedWindow = RecentAddedWindow.Days14,
                showNewTrackBadge = false,
            ),
        )
        val viewModel = recentlyAddedViewModel(homePreferencesRepository)
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(
            RecentlyAddedUiState.Success(
                tracks = listOf(recent),
                windowDays = 14,
                showNewTrackBadge = false,
            ),
            viewModel.uiState.value,
        )
    }

    private fun recentlyAddedViewModel(
        homePreferencesRepository: HomePreferencesRepository = FakeRecentlyAddedHomePreferencesRepository(),
    ) = RecentlyAddedViewModel(
        observeRecentlyAddedTracksUseCase = ObserveRecentlyAddedTracksUseCase(
            libraryRepository = repository,
            homePreferencesRepository = homePreferencesRepository,
            nowMillis = { NOW_MILLIS },
        ),
        observePlaybackStateUseCase = ObservePlaybackStateUseCase(playbackController),
        homePreferencesRepository = homePreferencesRepository,
        playTrackUseCase = PlayTrackUseCase(playbackController),
        playShuffledQueueUseCase = PlayShuffledQueueUseCase(playbackController),
    )

    private fun kotlinx.coroutines.CoroutineScope.startCollecting(
        viewModel: RecentlyAddedViewModel,
    ) {
        launch {
            viewModel.uiState.collect()
        }
    }

    private fun track(id: String, addedAtMillis: Long?) = Track(
        id = id,
        title = "Track $id",
        artist = Artist(id = "artist-$id", name = "Artist $id"),
        durationMillis = 180_000L,
        addedAtMillis = addedAtMillis,
        mediaUri = "content://media/external/audio/media/$id",
    )

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
        const val NOW_MILLIS = 100 * DAY_MILLIS
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecentlyAddedMainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeRecentlyAddedLibraryRepository : LibraryRepository {
    private val tracks = MutableStateFlow(emptyList<Track>())

    override fun observeAlbums() = MutableStateFlow(emptyList<Album>())
    override fun observeAlbumContent(albumId: String) = MutableStateFlow<AlbumContent?>(null)
    override fun observeArtists() = MutableStateFlow(emptyList<ArtistSummary>())
    override fun observeArtistContent(artistId: String) = MutableStateFlow<ArtistContent?>(null)
    override fun observeTracks() = tracks
    override fun observeAllTracks() = tracks
    override fun observeFolders() = MutableStateFlow(emptyList<LibraryFolder>())
    override fun observeFolderContent(folderId: String) =
        MutableStateFlow<LibraryFolderContent?>(null)

    override suspend fun resolvePlaylistSource(source: PlaylistSource) = emptyList<Track>()
    override suspend fun refreshTracks() = 0
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit

    fun setTracks(newTracks: List<Track>) {
        tracks.update { newTracks }
    }
}

private class FakeRecentlyAddedPlaybackController : PlaybackController {
    private val mutablePlaybackState = MutableStateFlow(PlaybackState())
    override val playbackState: Flow<PlaybackState> = mutablePlaybackState

    lateinit var playedTrack: Track
    lateinit var playedQueue: List<Track>
    var shuffleEnabled: Boolean? = null

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) {
        playedTrack = track
        playedQueue = queue
    }

    override suspend fun playQueueItem(index: Int) = Unit
    override suspend fun addQueueItem(track: Track) = Unit
    override suspend fun playNext(track: Track) = Unit
    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit
    override suspend fun removeQueueItem(index: Int) = Unit
    override suspend fun togglePlayPause() = Unit
    override suspend fun pause() = Unit
    override suspend fun skipNext() = Unit
    override suspend fun skipPrevious() = Unit
    override suspend fun seekTo(positionMillis: Long) = Unit

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
    }

    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) = Unit
    override suspend fun restoreLastSession() = Unit
    override suspend fun stop() = Unit
}

private class FakeRecentlyAddedHomePreferencesRepository(
    initialSettings: HomeRecommendationsSettings = HomeRecommendationsSettings(),
) : HomePreferencesRepository {
    val settings = MutableStateFlow(initialSettings)

    override fun observeHomeRecommendationsSettings(): Flow<HomeRecommendationsSettings> = settings

    override suspend fun setShowRecommendedPlaylists(show: Boolean) {
        settings.update { it.copy(showRecommendedPlaylists = show) }
    }

    override suspend fun setRecentAddedWindow(window: RecentAddedWindow) {
        settings.update { it.copy(recentAddedWindow = window) }
    }

    override suspend fun setShowNewTrackBadge(show: Boolean) {
        settings.update { it.copy(showNewTrackBadge = show) }
    }
}
