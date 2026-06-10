package com.catlytics.feature.library.impl.folder

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.usecase.ObserveFolderContentUseCase
import com.catlytics.core.domain.usecase.PlayTrackUseCase
import com.catlytics.core.model.Artist
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import com.catlytics.feature.library.impl.root.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryFolderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `opening folder exposes its content`() = runTest {
        val repository = FolderFakeLibraryRepository()
        val content = content()
        repository.content.value = content
        val viewModel = viewModel(repository, FolderFakePlaybackController())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.openFolder(FOLDER_ID)
        advanceUntilIdle()

        assertEquals(LibraryFolderUiState.Success(content), viewModel.uiState.value)
    }

    @Test
    fun `playing track uses direct folder tracks as queue`() = runTest {
        val playbackController = FolderFakePlaybackController()
        val viewModel = viewModel(FolderFakeLibraryRepository(), playbackController)
        val queue = listOf(track("one"), track("two"))

        viewModel.playTrack(queue[1], queue)
        advanceUntilIdle()

        assertEquals(queue[1], playbackController.playedTrack)
        assertEquals(queue, playbackController.playedQueue)
        assertEquals(1, playbackController.startIndex)
    }

    private fun viewModel(
        repository: FolderFakeLibraryRepository,
        playbackController: FolderFakePlaybackController,
    ) = LibraryFolderViewModel(
        observeFolderContentUseCase = ObserveFolderContentUseCase(repository),
        playTrackUseCase = PlayTrackUseCase(playbackController),
    )

    private fun content() = LibraryFolderContent(
        folder = LibraryFolder(FOLDER_ID, "Music", "Music", 1, isVisible = false),
        subfolders = emptyList(),
        tracks = listOf(track("one")),
    )

    private fun track(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = Artist("artist-$id", "Artist $id"),
        durationMillis = 180_000,
        mediaUri = "content://media/$id",
    )

    private companion object {
        const val FOLDER_ID = "external_primary:Music"
    }
}

private class FolderFakeLibraryRepository : LibraryRepository {
    val content = MutableStateFlow<LibraryFolderContent?>(null)

    override fun observeTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeAllTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeFolders() = MutableStateFlow(emptyList<LibraryFolder>())
    override fun observeFolderContent(folderId: String) = content
    override suspend fun refreshTracks() = Unit
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

private class FolderFakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = MutableStateFlow(PlaybackState())
    lateinit var playedTrack: Track
    lateinit var playedQueue: List<Track>
    var startIndex = -1

    override suspend fun play(track: Track, queue: List<Track>, startIndex: Int) {
        playedTrack = track
        playedQueue = queue
        this.startIndex = startIndex
    }

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
