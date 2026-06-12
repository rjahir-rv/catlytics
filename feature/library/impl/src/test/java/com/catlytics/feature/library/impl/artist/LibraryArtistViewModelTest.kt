package com.catlytics.feature.library.impl.artist

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.usecase.library.ObserveArtistContentUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import com.catlytics.feature.library.impl.root.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryArtistViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `opening artist exposes its content`() = runTest {
        val repository = ArtistFakeLibraryRepository()
        val content = artistContent()
        repository.content.value = content
        val viewModel = viewModel(repository, ArtistFakePlaybackController())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.openArtist(ARTIST_ID)
        advanceUntilIdle()

        assertEquals(LibraryArtistUiState.Success(content), viewModel.uiState.value)
    }

    @Test
    fun `playing track replaces queue with artist tracks`() = runTest {
        val playbackController = ArtistFakePlaybackController()
        val viewModel = viewModel(ArtistFakeLibraryRepository(), playbackController)
        val queue = listOf(track("one"), track("two"))

        viewModel.playTrack(queue[1], queue)
        advanceUntilIdle()

        assertEquals(queue[1], playbackController.playedTrack)
        assertEquals(queue, playbackController.playedQueue)
        assertEquals(1, playbackController.startIndex)
    }

    private fun viewModel(
        repository: ArtistFakeLibraryRepository,
        playbackController: ArtistFakePlaybackController,
    ) = LibraryArtistViewModel(
        observeArtistContentUseCase = ObserveArtistContentUseCase(repository),
        playTrackUseCase = PlayTrackUseCase(playbackController),
    )

    private fun artistContent(): ArtistContent {
        val tracks = listOf(track("one"))
        return ArtistContent(
            summary = ArtistSummary(Artist(ARTIST_ID, "Artist"), albumCount = 1, trackCount = 1),
            albums = listOf(Album("album", "Album", Artist(ARTIST_ID, "Artist"), trackCount = 1)),
            tracks = tracks,
        )
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = Artist(ARTIST_ID, "Artist"),
        durationMillis = 180_000L,
        mediaUri = "content://media/$id",
    )

    private companion object {
        const val ARTIST_ID = "artist-1"
    }
}

private class ArtistFakeLibraryRepository : LibraryRepository {
    val content = MutableStateFlow<ArtistContent?>(null)

    override fun observeAlbums() = MutableStateFlow(emptyList<Album>())
    override fun observeAlbumContent(albumId: String) = MutableStateFlow<AlbumContent?>(null)
    override fun observeArtists() = MutableStateFlow(emptyList<ArtistSummary>())
    override fun observeArtistContent(artistId: String) = content
    override fun observeTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeAllTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeFolders() = MutableStateFlow(emptyList<LibraryFolder>())
    override fun observeFolderContent(folderId: String) =
        MutableStateFlow<LibraryFolderContent?>(null)
    override suspend fun refreshTracks() = Unit
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

private class ArtistFakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = MutableStateFlow(PlaybackState())
    lateinit var playedTrack: Track
    lateinit var playedQueue: List<Track>
    var startIndex = -1

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
