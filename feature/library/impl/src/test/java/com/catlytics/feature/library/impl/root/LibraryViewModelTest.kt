package com.catlytics.feature.library.impl.root

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.usecase.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.ObserveLibraryFoldersUseCase
import com.catlytics.core.domain.usecase.RefreshLibraryUseCase
import com.catlytics.core.domain.usecase.SetFolderVisibilityUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `folders are exposed as success state`() = runTest {
        val repository = FakeLibraryRepository()
        val folder = folder()
        repository.folders.value = listOf(folder)
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect() }

        advanceUntilIdle()

        assertEquals(
            LibraryUiState.Success(albums = emptyList(), folders = listOf(folder)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `albums are exposed as success state`() = runTest {
        val repository = FakeLibraryRepository()
        val album = Album(
            id = "album-1",
            title = "Album",
            artist = com.catlytics.core.model.Artist("artist-1", "Artist"),
            trackCount = 2,
        )
        repository.albums.value = listOf(album)
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect() }

        advanceUntilIdle()

        assertEquals(
            LibraryUiState.Success(albums = listOf(album), folders = emptyList()),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `visibility change is delegated to repository`() = runTest {
        val repository = FakeLibraryRepository()
        val viewModel = viewModel(repository)

        viewModel.setFolderVisible(FOLDER_ID, visible = false)
        advanceUntilIdle()

        assertEquals(FOLDER_ID to false, repository.lastVisibilityChange)
    }

    @Test
    fun `refresh error is exposed`() = runTest {
        val repository = FakeLibraryRepository().apply {
            refreshResult = Result.failure(IllegalStateException("MediaStore failed"))
        }
        val viewModel = viewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.refreshLibraryOnce()
        advanceUntilIdle()

        assertEquals(LibraryUiState.Error("MediaStore failed"), viewModel.uiState.value)
    }

    private fun viewModel(repository: FakeLibraryRepository) = LibraryViewModel(
        observeAlbumsUseCase = ObserveAlbumsUseCase(repository),
        observeLibraryFoldersUseCase = ObserveLibraryFoldersUseCase(repository),
        refreshLibraryUseCase = RefreshLibraryUseCase(repository),
        setFolderVisibilityUseCase = SetFolderVisibilityUseCase(repository),
    )

    private fun folder() = LibraryFolder(
        id = FOLDER_ID,
        name = "Music",
        path = "Music",
        trackCount = 3,
        isVisible = true,
    )

    private companion object {
        const val FOLDER_ID = "external_primary:Music"
    }
}

private class FakeLibraryRepository : LibraryRepository {
    val albums = MutableStateFlow(emptyList<Album>())
    val folders = MutableStateFlow(emptyList<LibraryFolder>())
    var refreshResult: Result<Unit> = Result.success(Unit)
    var lastVisibilityChange: Pair<String, Boolean>? = null

    override fun observeAlbums() = albums
    override fun observeAlbumContent(albumId: String) = MutableStateFlow<AlbumContent?>(null)

    override fun observeTracks() = MutableStateFlow(emptyList<Track>())

    override fun observeAllTracks() = MutableStateFlow(emptyList<Track>())

    override fun observeFolders() = folders

    override fun observeFolderContent(folderId: String) =
        MutableStateFlow<LibraryFolderContent?>(null)

    override suspend fun refreshTracks() {
        refreshResult.getOrThrow()
    }

    override suspend fun setFolderVisible(folderId: String, visible: Boolean) {
        lastVisibilityChange = folderId to visible
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
