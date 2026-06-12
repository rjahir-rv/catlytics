package com.catlytics.feature.home.impl

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeLibraryRepository
    private lateinit var playbackController: FakePlaybackController

    @Before
    fun setUp() {
        repository = FakeLibraryRepository()
        playbackController = FakePlaybackController()
    }

    @Test
    fun `uiState is empty when library has no tracks`() = runTest {
        val viewModel = homeViewModel()
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(HomeUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `uiState is success when library has tracks`() = runTest {
        val track = track(id = "track-1")
        repository.setTracks(listOf(track))
        val viewModel = homeViewModel()
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(HomeUiState.Success(listOf(track)), viewModel.uiState.value)
    }

    @Test
    fun `refreshLibrary surfaces refresh errors`() = runTest {
        repository.refreshResult = Result.failure(IllegalStateException("MediaStore failed"))
        val viewModel = homeViewModel()
        backgroundScope.startCollecting(viewModel)

        viewModel.refreshLibrary()
        advanceUntilIdle()

        assertEquals(HomeUiState.Error("MediaStore failed"), viewModel.uiState.value)
    }

    @Test
    fun `uiState starts as loading`() = runTest {
        val viewModel = homeViewModel()

        assertTrue(viewModel.uiState.first() is HomeUiState.Loading)
    }

    @Test
    fun `onTrackSelected dispatches playback with selected track and queue`() = runTest {
        val queue = listOf(track("track-1"), track("track-2"), track("track-3"))
        val viewModel = homeViewModel()

        viewModel.onTrackSelected(queue[1], queue)
        advanceUntilIdle()

        assertEquals(queue[1], playbackController.playedTrack)
        assertEquals(queue, playbackController.playedQueue)
        assertEquals(1, playbackController.startIndex)
    }

    private fun homeViewModel() = HomeViewModel(
        observeLibraryUseCase = ObserveLibraryUseCase(repository),
        refreshLibraryUseCase = RefreshLibraryUseCase(repository),
        playTrackUseCase = PlayTrackUseCase(playbackController),
    )

    private fun kotlinx.coroutines.CoroutineScope.startCollecting(viewModel: HomeViewModel) {
        launch {
            viewModel.uiState.collect()
        }
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = Artist(
            id = "artist-$id",
            name = "Artist $id",
        ),
        durationMillis = 180_000L,
        mediaUri = "content://media/external/audio/media/$id",
    )
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

private class FakeLibraryRepository : LibraryRepository {
    override fun observeAlbums() = MutableStateFlow(emptyList<Album>())
    override fun observeAlbumContent(albumId: String) = MutableStateFlow<AlbumContent?>(null)
    override fun observeArtists() = MutableStateFlow(emptyList<ArtistSummary>())
    override fun observeArtistContent(artistId: String) = MutableStateFlow<ArtistContent?>(null)

    private val tracks = MutableStateFlow(emptyList<Track>())
    private val folders = MutableStateFlow(emptyList<LibraryFolder>())
    var refreshResult: Result<Unit> = Result.success(Unit)

    override fun observeTracks() = tracks

    override fun observeAllTracks() = tracks

    override fun observeFolders() = folders

    override fun observeFolderContent(folderId: String) =
        MutableStateFlow<LibraryFolderContent?>(null)

    override suspend fun resolvePlaylistSource(source: com.catlytics.core.model.PlaylistSource) =
        emptyList<Track>()

    override suspend fun refreshTracks() {
        refreshResult.getOrThrow()
    }

    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit

    fun setTracks(newTracks: List<Track>) {
        tracks.update { newTracks }
    }
}

private class FakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = MutableStateFlow(PlaybackState())

    lateinit var playedTrack: Track
    lateinit var playedQueue: List<Track>
    var startIndex: Int = -1

    override suspend fun play(track: Track, queue: List<Track>, startIndex: Int) {
        playedTrack = track
        playedQueue = queue
        this.startIndex = startIndex
    }

    override suspend fun playQueueItem(index: Int) = Unit

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit

    override suspend fun togglePlayPause() = Unit

    override suspend fun pause() = Unit

    override suspend fun skipNext() = Unit

    override suspend fun skipPrevious() = Unit

    override suspend fun seekTo(positionMillis: Long) = Unit

    override suspend fun setShuffleEnabled(enabled: Boolean) = Unit

    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) = Unit

    override suspend fun restoreLastSession() = Unit

    override suspend fun stop() = Unit
}
