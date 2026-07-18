package com.catlytics.app.ui

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AppStartupViewModelTest {
    @get:Rule
    val mainDispatcherRule = StartupMainDispatcherRule()

    @Test
    fun `permission starts refresh and exposes loading until it completes`() = runTest {
        val refreshGate = CompletableDeferred<Unit>()
        val repository = FakeStartupLibraryRepository {
            refreshGate.await()
        }
        val viewModel = AppStartupViewModel(RefreshLibraryUseCase(repository))

        assertEquals(AppStartupUiState.WaitingForPermission, viewModel.uiState.value)

        viewModel.onAudioPermissionState(hasAudioPermission = true)
        assertEquals(AppStartupUiState.Loading, viewModel.uiState.value)

        refreshGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(AppStartupUiState.Ready, viewModel.uiState.value)
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `refresh failure exposes its message`() = runTest {
        val repository = FakeStartupLibraryRepository {
            error("MediaStore failed")
        }
        val viewModel = AppStartupViewModel(RefreshLibraryUseCase(repository))

        viewModel.onAudioPermissionState(hasAudioPermission = true)
        advanceUntilIdle()

        assertEquals(
            AppStartupUiState.Error("MediaStore failed"),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `refresh runs only once for repeated permission updates`() = runTest {
        val repository = FakeStartupLibraryRepository()
        val viewModel = AppStartupViewModel(RefreshLibraryUseCase(repository))

        viewModel.onAudioPermissionState(hasAudioPermission = false)
        viewModel.onAudioPermissionState(hasAudioPermission = true)
        viewModel.onAudioPermissionState(hasAudioPermission = true)
        advanceUntilIdle()
        viewModel.onAudioPermissionState(hasAudioPermission = true)
        advanceUntilIdle()

        assertEquals(AppStartupUiState.Ready, viewModel.uiState.value)
        assertEquals(1, repository.refreshCalls)
    }
}

private class FakeStartupLibraryRepository(
    private val onRefresh: suspend () -> Unit = {},
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

    override suspend fun refreshTracks() {
        refreshCalls++
        onRefresh()
    }

    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class StartupMainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
