package com.catlytics.feature.playlists.impl

import com.catlytics.core.domain.usecase.playback.ObserveNowPlayingUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import com.catlytics.core.domain.usecase.playlist.DeletePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistSortDirectionUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistSummariesUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistViewModeUseCase
import com.catlytics.core.domain.usecase.playlist.RenamePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistCoverUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistSortDirectionUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistViewModeUseCase
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistViewMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playlists = FakePlaylistRepository(
        Playlist(id = "focus", name = "Focus", trackIds = listOf("a", "b", "missing")),
        Playlist(id = "gym", name = "Gym", trackIds = emptyList()),
    )
    private val library = FakeLibraryRepository(track("a", 100_000L), track("b", 200_000L))
    private val controller = FakePlaybackController()
    private val preferences = FakeLibraryPreferencesRepository()

    @Test
    fun `summaries count only the tracks still in the library`() = runTest {
        val content = contentOf(viewModel())

        val focus = content.playlists.first { it.playlist.id == "focus" }
        assertEquals(2, focus.trackCount)
        assertEquals(300_000L, focus.totalDurationMillis)
        assertEquals(0, content.playlists.first { it.playlist.id == "gym" }.trackCount)
    }

    @Test
    fun `renaming to another playlist's name is ignored`() = runTest {
        val viewModel = viewModel()
        contentOf(viewModel)

        viewModel.rename("gym", "  focus ")
        advanceUntilIdle()
        assertEquals("Gym", playlists.playlist("gym")?.name)

        viewModel.rename("gym", "  Cardio ")
        advanceUntilIdle()
        assertEquals("Cardio", playlists.playlist("gym")?.name)
    }

    @Test
    fun `renaming a playlist to its own name with other casing is allowed`() = runTest {
        val viewModel = viewModel()
        contentOf(viewModel)

        viewModel.rename("focus", "FOCUS")
        advanceUntilIdle()

        assertEquals("FOCUS", playlists.playlist("focus")?.name)
    }

    @Test
    fun `play starts from the first available track with the playlist as source`() = runTest {
        val viewModel = viewModel()

        viewModel.play("focus")
        viewModel.play("gym")
        advanceUntilIdle()

        val request = controller.playRequests.single()
        assertEquals("a", request.track.id)
        assertEquals(listOf("a", "b"), request.queue.map { it.id })
        assertEquals(PlaybackQueueSource.Playlist("focus"), request.queueSource)
    }

    @Test
    fun `play shuffled enables shuffle and ignores empty playlists`() = runTest {
        val viewModel = viewModel()

        viewModel.playShuffled("gym")
        advanceUntilIdle()
        assertTrue(controller.playRequests.isEmpty())

        viewModel.playShuffled("focus")
        advanceUntilIdle()
        assertTrue(controller.shuffleEnabled)
        assertEquals(PlaybackQueueSource.Playlist("focus"), controller.playRequests.single().queueSource)
    }

    @Test
    fun `active playlist follows the playback source and status`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.activePlaylist.collect() }

        controller.playbackState.value = PlaybackState(
            status = PlaybackStatus.Buffering,
            currentTrack = track("a"),
            queueSource = PlaybackQueueSource.Playlist("focus"),
        )
        advanceUntilIdle()
        assertEquals(ActivePlaylist("focus", isPlaying = true), viewModel.activePlaylist.value)

        controller.playbackState.value = controller.playbackState.value.copy(status = PlaybackStatus.Paused)
        advanceUntilIdle()
        assertEquals(ActivePlaylist("focus", isPlaying = false), viewModel.activePlaylist.value)

        controller.playbackState.value = controller.playbackState.value.copy(
            queueSource = PlaybackQueueSource.Static,
        )
        advanceUntilIdle()
        assertNull(viewModel.activePlaylist.value)
    }

    @Test
    fun `view mode and delete go through the repositories`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.viewMode.collect() }

        viewModel.setViewMode(PlaylistViewMode.Mosaic)
        viewModel.delete("gym")
        advanceUntilIdle()

        assertEquals(PlaylistViewMode.Mosaic, viewModel.viewMode.value)
        assertNull(playlists.playlist("gym"))
    }

    private suspend fun TestScope.contentOf(viewModel: PlaylistsViewModel): PlaylistsUiState.Content {
        backgroundScope.launch { viewModel.uiState.collect() }
        // Los resúmenes se calculan en Dispatchers.Default, fuera del reloj virtual.
        return viewModel.uiState.first { it is PlaylistsUiState.Content } as PlaylistsUiState.Content
    }

    private fun viewModel() = PlaylistsViewModel(
        observePlaylistSummariesUseCase = ObservePlaylistSummariesUseCase(playlists, library),
        observePlaylistViewModeUseCase = ObservePlaylistViewModeUseCase(preferences),
        observePlaylistSortDirectionUseCase = ObservePlaylistSortDirectionUseCase(preferences),
        observeNowPlayingUseCase = ObserveNowPlayingUseCase(controller),
        observePlaylistContentUseCase = ObservePlaylistContentUseCase(playlists, library),
        playTrackUseCase = PlayTrackUseCase(controller),
        playShuffledQueueUseCase = PlayShuffledQueueUseCase(controller),
        togglePlaybackUseCase = TogglePlaybackUseCase(controller),
        renamePlaylistUseCase = RenamePlaylistUseCase(playlists),
        deletePlaylistUseCase = DeletePlaylistUseCase(playlists),
        setPlaylistViewModeUseCase = SetPlaylistViewModeUseCase(preferences),
        setPlaylistSortDirectionUseCase = SetPlaylistSortDirectionUseCase(preferences),
        setPlaylistCoverUseCase = SetPlaylistCoverUseCase(playlists),
    )
}
