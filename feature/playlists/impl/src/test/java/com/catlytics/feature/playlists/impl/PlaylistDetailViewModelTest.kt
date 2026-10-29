package com.catlytics.feature.playlists.impl

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.playback.ObserveNowPlayingUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import com.catlytics.core.domain.usecase.playlist.AddToPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.DeletePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ExportPlaylistToM3uUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.playlist.RemoveTrackFromPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ReorderPlaylistTracksUseCase
import com.catlytics.core.domain.usecase.playlist.RestorePlaylistTracksUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistCoverUseCase
import com.catlytics.core.domain.usecase.playlist.UpdatePlaylistDetailsUseCase
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.Playlist
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tracks = listOf(track("a"), track("b"), track("c"))
    private val playlists = FakePlaylistRepository(
        Playlist(id = "focus", name = "Focus", trackIds = listOf("c", "a", "missing")),
    )
    private val library = FakeLibraryRepository(*tracks.toTypedArray())
    private val controller = FakePlaybackController()
    private val exporter = FakePlaylistExportRepository()

    @Test
    fun `stays loading until a playlist is opened`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(PlaylistDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `opening a playlist resolves its tracks in playlist order`() = runTest {
        val viewModel = openedViewModel("focus")

        val content = (viewModel.uiState.value as PlaylistDetailUiState.Success).content
        assertEquals(listOf("c", "a"), content.tracks.map { it.id })
    }

    @Test
    fun `an unknown playlist is not found`() = runTest {
        val viewModel = openedViewModel("deleted")

        assertEquals(PlaylistDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun `removing tracks reports the count and undo restores the previous order`() = runTest {
        val viewModel = openedViewModel("focus")
        val effects = collectEffects(viewModel)

        viewModel.remove(listOf("c", "a"))
        advanceUntilIdle()

        val removed = effects.single() as PlaylistDetailEffect.TracksRemoved
        assertEquals(UiText.Plural(R.plurals.playlist_detail_tracks_removed, 2), removed.text)
        assertEquals(listOf("missing"), playlists.playlist("focus")?.trackIds)

        viewModel.undoRemove(removed.previousTrackIds)
        advanceUntilIdle()

        assertEquals(listOf("c", "a", "missing"), playlists.playlist("focus")?.trackIds)
    }

    @Test
    fun `removing tracks that are not in the playlist reports nothing`() = runTest {
        val viewModel = openedViewModel("focus")
        val effects = collectEffects(viewModel)

        viewModel.remove("b")
        advanceUntilIdle()

        assertTrue(effects.isEmpty())
    }

    @Test
    fun `an order with duplicates is rejected with a generic error`() = runTest {
        val viewModel = openedViewModel("focus")
        val effects = collectEffects(viewModel)
        var saved = false

        viewModel.saveOrder(listOf("a", "a")) { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertEquals(listOf("c", "a", "missing"), playlists.playlist("focus")?.trackIds)
        assertEquals(genericError, effects.single())
    }

    @Test
    fun `saving details trims them and only changes the cover when asked`() = runTest {
        val viewModel = openedViewModel("focus")
        var saved = 0

        viewModel.saveDetails("  Deep focus ", " Para estudiar ", "content://cover", artworkChanged = false) {
            saved++
        }
        advanceUntilIdle()

        val playlist = playlists.playlist("focus")!!
        assertEquals("Deep focus", playlist.name)
        assertEquals("Para estudiar", playlist.description)
        assertNull(playlist.artworkUri)

        viewModel.saveDetails("Deep focus", "", "content://cover", artworkChanged = true) { saved++ }
        advanceUntilIdle()

        assertEquals("content://cover", playlists.playlist("focus")?.artworkUri)
        assertEquals(2, saved)
    }

    @Test
    fun `a blank name is rejected with a generic error`() = runTest {
        val viewModel = openedViewModel("focus")
        val effects = collectEffects(viewModel)

        viewModel.saveDetails("   ", "", null, artworkChanged = false) {}
        advanceUntilIdle()

        assertEquals("Focus", playlists.playlist("focus")?.name)
        assertEquals(genericError, effects.single())
    }

    @Test
    fun `deleting emits deleted and a failure emits an error instead`() = runTest {
        val viewModel = openedViewModel("focus")
        val effects = collectEffects(viewModel)

        playlists.failure = IllegalStateException("disk full")
        viewModel.delete()
        advanceUntilIdle()
        assertEquals(genericError, effects.last())

        playlists.failure = null
        viewModel.delete()
        advanceUntilIdle()
        assertEquals(PlaylistDetailEffect.Deleted, effects.last())
        assertNull(playlists.playlist("focus"))
    }

    @Test
    fun `adding tracks reports only the new ones`() = runTest {
        val viewModel = openedViewModel("focus")
        val effects = collectEffects(viewModel)

        viewModel.addTracks(listOf("a", "b"), title = "Selección")
        advanceUntilIdle()

        assertEquals(
            PlaylistDetailEffect.Message(UiText.Plural(R.plurals.playlist_detail_tracks_added, 1)),
            effects.single(),
        )
        assertEquals(listOf("c", "a", "missing", "b"), playlists.playlist("focus")?.trackIds)
    }

    @Test
    fun `a failure while adding tracks shows the add error`() = runTest {
        library.resolveFailure = IllegalStateException("library unavailable")
        val viewModel = openedViewModel("focus")
        val effects = collectEffects(viewModel)

        viewModel.addTracks(listOf("b"), title = "Selección")
        advanceUntilIdle()

        assertEquals(
            PlaylistDetailEffect.Message(UiText.Resource(R.string.playlist_detail_error_add_tracks)),
            effects.single(),
        )
    }

    @Test
    fun `export reports success or failure`() = runTest {
        val viewModel = openedViewModel("focus")
        val effects = collectEffects(viewModel)

        viewModel.exportM3u("content://export.m3u8")
        advanceUntilIdle()
        exporter.result = Result.failure(IllegalStateException("no space"))
        viewModel.exportM3u("content://export.m3u8")
        advanceUntilIdle()

        assertEquals(
            listOf(
                PlaylistDetailEffect.Message(UiText.Resource(R.string.playlist_detail_export_success)),
                PlaylistDetailEffect.Message(UiText.Resource(R.string.playlist_detail_error_export)),
            ),
            effects,
        )
        assertEquals("focus" to "content://export.m3u8", exporter.exports.first())
    }

    @Test
    fun `playing from the detail uses the playlist as queue source`() = runTest {
        val viewModel = openedViewModel("focus")

        viewModel.play(tracks[2], listOf(tracks[2], tracks[0]))
        advanceUntilIdle()

        val request = controller.playRequests.single()
        assertEquals("c", request.track.id)
        assertEquals(PlaybackQueueSource.Playlist("focus"), request.queueSource)
    }

    private val genericError =
        PlaylistDetailEffect.Message(UiText.Resource(R.string.playlist_detail_error_generic))

    private fun TestScope.openedViewModel(id: String): PlaylistDetailViewModel {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect() }
        viewModel.open(id)
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.collectEffects(viewModel: PlaylistDetailViewModel): List<PlaylistDetailEffect> {
        val effects = mutableListOf<PlaylistDetailEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects += it }
        }
        return effects
    }

    private fun viewModel() = PlaylistDetailViewModel(
        observeContent = ObservePlaylistContentUseCase(playlists, library),
        removeTrack = RemoveTrackFromPlaylistUseCase(playlists),
        playTrack = PlayTrackUseCase(controller),
        playShuffledQueue = PlayShuffledQueueUseCase(controller),
        observeNowPlaying = ObserveNowPlayingUseCase(controller),
        togglePlaybackUseCase = TogglePlaybackUseCase(controller),
        updatePlaylistDetails = UpdatePlaylistDetailsUseCase(playlists),
        setPlaylistCover = SetPlaylistCoverUseCase(playlists),
        deletePlaylist = DeletePlaylistUseCase(playlists),
        reorderPlaylistTracks = ReorderPlaylistTracksUseCase(playlists),
        restorePlaylistTracks = RestorePlaylistTracksUseCase(playlists),
        observeLibrary = ObserveLibraryUseCase(library),
        addToPlaylist = AddToPlaylistUseCase(playlists, library),
        exportPlaylistToM3u = ExportPlaylistToM3uUseCase(exporter),
    )
}
