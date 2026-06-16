package com.catlytics.feature.library.impl.album

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.usecase.library.ObserveAlbumContentUseCase
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
class LibraryAlbumViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `opening album exposes its content`() = runTest {
        val repository = AlbumFakeLibraryRepository()
        val content = albumContent()
        repository.content.value = content
        val viewModel = viewModel(repository, AlbumFakePlaybackController())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.openAlbum(ALBUM_ID)
        advanceUntilIdle()

        assertEquals(LibraryAlbumUiState.Success(content), viewModel.uiState.value)
    }

    @Test
    fun `playing track replaces queue with album tracks`() = runTest {
        val playbackController = AlbumFakePlaybackController()
        val viewModel = viewModel(AlbumFakeLibraryRepository(), playbackController)
        val queue = listOf(track("one"), track("two"))

        viewModel.playTrack(queue[1], queue)
        advanceUntilIdle()

        assertEquals(queue[1], playbackController.playedTrack)
        assertEquals(queue, playbackController.playedQueue)
        assertEquals(1, playbackController.startIndex)
    }

    private fun viewModel(
        repository: AlbumFakeLibraryRepository,
        playbackController: AlbumFakePlaybackController,
    ) = LibraryAlbumViewModel(
        observeAlbumContentUseCase = ObserveAlbumContentUseCase(repository),
        playTrackUseCase = PlayTrackUseCase(playbackController),
    )

    private fun albumContent() = AlbumContent(
        album = Album(ALBUM_ID, "Album", Artist("artist", "Artist"), trackCount = 1),
        tracks = listOf(track("one")),
    )

    private fun track(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = Artist("artist", "Artist"),
        durationMillis = 180_000L,
        mediaUri = "content://media/$id",
    )

    private companion object {
        const val ALBUM_ID = "album-1"
    }
}

private class AlbumFakeLibraryRepository : LibraryRepository {
    val content = MutableStateFlow<AlbumContent?>(null)

    override fun observeAlbums() = MutableStateFlow(emptyList<Album>())
    override fun observeAlbumContent(albumId: String) = content
    override fun observeArtists() = MutableStateFlow(emptyList<ArtistSummary>())
    override fun observeArtistContent(artistId: String) = MutableStateFlow<ArtistContent?>(null)
    override fun observeTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeAllTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeFolders() = MutableStateFlow(emptyList<LibraryFolder>())
    override fun observeFolderContent(folderId: String) =
        MutableStateFlow<LibraryFolderContent?>(null)
    override suspend fun resolvePlaylistSource(source: com.catlytics.core.model.PlaylistSource) =
        emptyList<Track>()
    override suspend fun refreshTracks() = Unit
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

private class AlbumFakePlaybackController : PlaybackController {
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
    override suspend fun removeQueueItem(index: Int) = Unit
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
