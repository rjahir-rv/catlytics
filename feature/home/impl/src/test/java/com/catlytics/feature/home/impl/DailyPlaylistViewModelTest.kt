package com.catlytics.feature.home.impl

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.usecase.home.GenerateDailyPlaylistUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.playback.ObserveNowPlayingUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailyPlaylistViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val library = FakeDailyLibraryRepository()
    private val controller = FakeDailyPlaybackController()
    private val today = LocalDate.of(2026, 10, 28)
    private val generateDailyPlaylist = GenerateDailyPlaylistUseCase { today }

    @Test
    fun `a library smaller than the minimum has no daily playlist`() = runTest {
        library.tracks.value = tracks(GenerateDailyPlaylistUseCase.MIN_TRACK_COUNT - 1)
        val viewModel = viewModel()
        assertEquals(DailyPlaylistUiState.Loading, viewModel.uiState.value)

        startCollecting(viewModel)

        assertEquals(DailyPlaylistUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `a failing library shows the empty state instead of crashing`() = runTest {
        library.failure = IllegalStateException("permission revoked")
        val viewModel = viewModel()

        startCollecting(viewModel)

        assertEquals(DailyPlaylistUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `the daily playlist is the one generated for today and marks the current track`() = runTest {
        val allTracks = tracks(20)
        library.tracks.value = allTracks
        val expected = generateDailyPlaylist(allTracks)
        controller.playbackState.value = PlaybackState(
            status = PlaybackStatus.Playing,
            currentTrack = expected[1],
        )
        val viewModel = viewModel()

        startCollecting(viewModel)

        assertEquals(
            DailyPlaylistUiState.Success(
                tracks = expected,
                currentTrackId = expected[1].id,
                isCurrentTrackPlaying = true,
            ),
            viewModel.uiState.value,
        )
        assertEquals(GenerateDailyPlaylistUseCase.MAX_TRACK_COUNT, expected.size)
    }

    @Test
    fun `play all starts the daily playlist from its first track`() = runTest {
        library.tracks.value = tracks(8)
        val viewModel = viewModel()
        startCollecting(viewModel)
        val daily = (viewModel.uiState.value as DailyPlaylistUiState.Success).tracks

        viewModel.onPlayAll()
        advanceUntilIdle()

        val request = controller.playRequests.single()
        assertEquals(daily.first(), request.track)
        assertEquals(daily, request.queue)
        assertEquals(0, request.startIndex)
    }

    @Test
    fun `nothing plays before the daily playlist is ready`() = runTest {
        library.tracks.value = tracks(8)
        val viewModel = viewModel()

        viewModel.onPlayAll()
        viewModel.onShuffle()
        advanceUntilIdle()

        assertTrue(controller.playRequests.isEmpty())
    }

    @Test
    fun `shuffle plays the daily playlist with shuffle enabled`() = runTest {
        library.tracks.value = tracks(8)
        val viewModel = viewModel()
        startCollecting(viewModel)
        val daily = (viewModel.uiState.value as DailyPlaylistUiState.Success).tracks

        viewModel.onShuffle()
        advanceUntilIdle()

        assertTrue(controller.shuffleEnabled)
        assertEquals(daily, controller.playRequests.single().queue)
    }

    private fun TestScope.startCollecting(viewModel: DailyPlaylistViewModel) {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()
    }

    private fun viewModel() = DailyPlaylistViewModel(
        observeLibraryUseCase = ObserveLibraryUseCase(library),
        observeNowPlayingUseCase = ObserveNowPlayingUseCase(controller),
        generateDailyPlaylistUseCase = generateDailyPlaylist,
        playTrackUseCase = PlayTrackUseCase(controller),
        playShuffledQueueUseCase = PlayShuffledQueueUseCase(controller),
    )

    private fun tracks(count: Int) = List(count) { index ->
        Track(
            id = "track-$index",
            title = "Track $index",
            artist = Artist(id = "artist", name = "Frédéric Chopin"),
            durationMillis = 180_000L,
            mediaUri = "content://media/external/audio/media/$index",
        )
    }
}

private class FakeDailyLibraryRepository : LibraryRepository {
    val tracks = MutableStateFlow(emptyList<Track>())
    var failure: Exception? = null

    override fun observeTracks(): Flow<List<Track>> = flow {
        failure?.let { throw it }
        tracks.collect { emit(it) }
    }

    override fun observeAlbums(): Flow<List<Album>> = flowOf(emptyList())
    override fun observeAlbumContent(albumId: String): Flow<AlbumContent?> = flowOf(null)
    override fun observeArtists(): Flow<List<ArtistSummary>> = flowOf(emptyList())
    override fun observeArtistContent(artistId: String): Flow<ArtistContent?> = flowOf(null)
    override fun observeAllTracks(): Flow<List<Track>> = tracks
    override fun observeFolders(): Flow<List<LibraryFolder>> = flowOf(emptyList())
    override fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?> = flowOf(null)
    override suspend fun resolvePlaylistSource(source: PlaylistSource): List<Track> = emptyList()
    override suspend fun refreshTracks(): Int = 0
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

private data class DailyPlayRequest(val track: Track, val queue: List<Track>, val startIndex: Int)

private class FakeDailyPlaybackController : PlaybackController {
    override val playbackState = MutableStateFlow(PlaybackState())
    val playRequests = mutableListOf<DailyPlayRequest>()
    var shuffleEnabled = false

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) {
        playRequests += DailyPlayRequest(track, queue, startIndex)
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
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
    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) = Unit
    override suspend fun restoreLastSession() = Unit
    override suspend fun stop() = Unit
}
