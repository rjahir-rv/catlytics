package com.catlytics.feature.library.impl.metadata

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveTracksWithOverridesUseCase
import com.catlytics.core.domain.usecase.library.SaveBulkTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.WriteTracksTagsToFilesUseCase
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.BulkTrackMetadataEdit
import com.catlytics.core.model.FieldEdit
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.feature.library.impl.R
import com.catlytics.feature.library.impl.root.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class EditTracksMetadataViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val metadataRepository = FakeTrackMetadataRepository()
    private val libraryRepository = MetadataFakeLibraryRepository()

    @Test
    fun `shared values are prefilled and differing ones are marked as mixed`() = runTest {
        val viewModel = openedViewModel(
            track("1", artist = "Nova", album = "Debut"),
            track("2", artist = "Luz", album = "Debut"),
        )

        val state = viewModel.editing()
        assertEquals(2, state.trackCount)
        assertEquals("", state.form.artist.value)
        assertTrue(state.artistIsMixed)
        assertEquals("Debut", state.form.album.value)
        assertFalse(state.albumIsMixed)
        assertFalse(state.canSave)
    }

    @Test
    fun `saving sets only the changed fields`() = runTest {
        val viewModel = openedViewModel(
            track("1", artist = "Nova", album = "Debut"),
            track("2", artist = "Luz", album = "Debut"),
        )
        val events = mutableListOf<EditTracksMetadataEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect(events::add)
        }

        viewModel.onAlbumChange(" Recopilatorio ")
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            listOf("1", "2") to BulkTrackMetadataEdit(albumTitle = FieldEdit.Set("Recopilatorio")),
            metadataRepository.savedBulkEdit,
        )
        assertEquals(
            listOf(
                EditTracksMetadataEvent.Message(UiText.Plural(R.plurals.library_bulk_edit_saved, 2)),
                EditTracksMetadataEvent.Close,
            ),
            events,
        )
    }

    @Test
    fun `restoring a field is offered only when some track has it edited`() = runTest {
        metadataRepository.overrides.value = listOf(artistOverride("2"))
        val viewModel = openedViewModel(track("1"), track("2"))

        val state = viewModel.editing()
        assertTrue(state.artistHasEdits)
        assertFalse(state.albumHasEdits)

        viewModel.onRestoreArtist()
        viewModel.onArtworkPicked("content://picked")
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            BulkTrackMetadataEdit(
                artistName = FieldEdit.Restore,
                artwork = ArtworkEdit.Replace("content://picked"),
            ),
            metadataRepository.savedBulkEdit?.second,
        )
    }

    @Test
    fun `writing to files asks one permission for every track and then writes them`() = runTest {
        val viewModel = openedViewModel(track("1"), track("2"))
        val events = mutableListOf<EditTracksMetadataEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect(events::add)
        }

        viewModel.onWriteToFileChange(true)
        advanceUntilIdle()
        assertTrue(viewModel.editing().canSave)
        viewModel.save()
        advanceUntilIdle()
        assertEquals(
            EditTracksMetadataEvent.RequestFileWrite(listOf("content://media/1", "content://media/2")),
            events.single(),
        )

        viewModel.onFileWritePermissionResult(granted = true)
        advanceUntilIdle()

        assertTrue(metadataRepository.wroteFile)
        assertEquals(
            listOf(
                EditTracksMetadataEvent.Message(UiText.Plural(R.plurals.library_bulk_edit_files_saved, 2)),
                EditTracksMetadataEvent.Close,
            ),
            events.drop(1),
        )
    }

    @Test
    fun `denied file permission keeps the edits only in the app`() = runTest {
        val viewModel = openedViewModel(track("1"), track("2"))
        val events = mutableListOf<EditTracksMetadataEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect(events::add)
        }

        viewModel.onFileWritePermissionResult(granted = false)
        advanceUntilIdle()

        assertFalse(metadataRepository.wroteFile)
        assertEquals(
            EditTracksMetadataEvent.Message(UiText.Resource(R.string.library_edit_file_denied)),
            events.first(),
        )
    }

    @Test
    fun `tracks missing from the library are not editable`() = runTest {
        val viewModel = openedViewModel()

        assertEquals(EditTracksMetadataUiState.NotFound, viewModel.uiState.value)
        assertNull(metadataRepository.savedBulkEdit)
    }

    private fun TestScope.openedViewModel(vararg tracks: Track): EditTracksMetadataViewModel {
        libraryRepository.allTracks.value = tracks.toList()
        val viewModel = EditTracksMetadataViewModel(
            observeTracksWithOverrides = ObserveTracksWithOverridesUseCase(
                metadataRepository,
                libraryRepository,
            ),
            observeArtists = ObserveArtistsUseCase(libraryRepository),
            observeAlbums = ObserveAlbumsUseCase(libraryRepository),
            saveBulkTrackMetadata = SaveBulkTrackMetadataUseCase(metadataRepository),
            writeTracksTagsToFiles = WriteTracksTagsToFilesUseCase(metadataRepository),
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.open(listOf("1", "2"))
        advanceUntilIdle()
        return viewModel
    }

    private fun EditTracksMetadataViewModel.editing() =
        uiState.value as EditTracksMetadataUiState.Editing

    private fun track(id: String, artist: String = "Nova", album: String = "Debut") = Track(
        id = id,
        title = "Pista $id",
        artist = Artist("artist-$artist", artist),
        durationMillis = 1_000L,
        mediaUri = "content://media/$id",
        albumId = "album-$album",
        albumTitle = album,
    )

    private fun artistOverride(id: String) = TrackMetadataOverride(
        trackId = id,
        fileKey = null,
        artistName = "Nova",
        originalTitle = "Pista $id",
        originalArtistName = "Otro",
        originalAlbumTitle = "Debut",
        durationMillis = 1_000L,
        updatedAtMillis = 1L,
    )
}
