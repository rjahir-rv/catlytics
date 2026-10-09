package com.catlytics.feature.library.impl.metadata

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.TrackMetadataRepository
import com.catlytics.core.domain.usecase.library.MergeArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveEditableTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.ResetTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.SaveTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.WriteTrackTagsToFileUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackMetadataEdit
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.feature.library.impl.R
import com.catlytics.feature.library.impl.root.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
class EditTrackMetadataViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val metadataRepository = FakeTrackMetadataRepository()
    private val libraryRepository = MetadataFakeLibraryRepository()
    private val identityRepository = MetadataFakeArtistIdentityRepository()

    @Test
    fun `opening a track fills the form with its stored values`() = runTest {
        metadataRepository.overrides.value = listOf(override(title = "Editada"))
        val viewModel = openedViewModel()

        val state = viewModel.editing()
        assertEquals("Editada", state.form.title)
        assertEquals("Artista desconocido", state.form.artistName)
        assertEquals("Álbum desconocido", state.form.albumTitle)
        assertTrue(state.hasOverride)
    }

    @Test
    fun `blank title cannot be saved`() = runTest {
        val viewModel = openedViewModel()

        viewModel.onTitleChange("  ")
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        assertFalse(viewModel.editing().canSave)
        assertNull(metadataRepository.savedEdit)
    }

    @Test
    fun `saving stores the edit and closes the sheet`() = runTest {
        val viewModel = openedViewModel()
        val events = mutableListOf<EditTrackMetadataEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect(events::add)
        }

        viewModel.onTitleChange("Intro")
        viewModel.onArtistChange("Nova")
        viewModel.onAlbumChange("Debut")
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(TrackMetadataEdit("Intro", "Nova", "Debut", ArtworkEdit.Keep), metadataRepository.savedEdit)
        assertEquals(
            listOf(
                EditTrackMetadataEvent.Message(UiText.Resource(R.string.library_edit_saved)),
                EditTrackMetadataEvent.Close,
            ),
            events,
        )
    }

    @Test
    fun `typing an existing artist suggests it and offers unifying the original one`() = runTest {
        libraryRepository.artists.value = listOf(
            ArtistSummary(NOVA, albumCount = 1, trackCount = 3),
            ArtistSummary(ORIGINAL_ARTIST, albumCount = 1, trackCount = 2),
        )
        val viewModel = openedViewModel(original = track(artist = ORIGINAL_ARTIST))

        viewModel.onArtistChange("nov")
        advanceUntilIdle()
        assertEquals(listOf("Nova", "Nova feat. Luz"), viewModel.editing().artistSuggestions)

        viewModel.onArtistChange("Nova")
        viewModel.onUnifyArtistChange(true)
        advanceUntilIdle()
        assertEquals(NOVA, viewModel.editing().unifyTarget)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf(ArtistAlias(ORIGINAL_ARTIST, NOVA)), identityRepository.aliases.value)
        // The merge moves the track, so its own artist is left as scanned.
        assertEquals("Nova feat. Luz", metadataRepository.savedEdit?.artistName)
    }

    @Test
    fun `unknown artist is never offered for unification`() = runTest {
        libraryRepository.artists.value = listOf(ArtistSummary(NOVA, albumCount = 1, trackCount = 3))
        val viewModel = openedViewModel()

        viewModel.onArtistChange("Nova")
        advanceUntilIdle()

        assertNull(viewModel.editing().unifyTarget)
    }

    @Test
    fun `writing to file asks for permission and reports a denial`() = runTest {
        val viewModel = openedViewModel()
        val events = mutableListOf<EditTrackMetadataEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect(events::add)
        }

        viewModel.onWriteToFileChange(true)
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()
        assertEquals(EditTrackMetadataEvent.RequestFileWrite("content://media/1"), events.single())

        viewModel.onFileWritePermissionResult(granted = false)
        advanceUntilIdle()

        assertFalse(metadataRepository.wroteFile)
        assertEquals(
            EditTrackMetadataEvent.Message(UiText.Resource(R.string.library_edit_file_denied)),
            events[1],
        )
    }

    @Test
    fun `granted permission writes the tags`() = runTest {
        val viewModel = openedViewModel()

        viewModel.onFileWritePermissionResult(granted = true)
        advanceUntilIdle()

        assertTrue(metadataRepository.wroteFile)
    }

    private fun TestScope.openedViewModel(original: Track = track()): EditTrackMetadataViewModel {
        metadataRepository.original.value = original
        val viewModel = EditTrackMetadataViewModel(
            observeEditableTrackMetadata = ObserveEditableTrackMetadataUseCase(
                metadataRepository,
                libraryRepository,
            ),
            observeArtists = ObserveArtistsUseCase(libraryRepository),
            observeAlbums = ObserveAlbumsUseCase(libraryRepository),
            saveTrackMetadata = SaveTrackMetadataUseCase(metadataRepository),
            resetTrackMetadata = ResetTrackMetadataUseCase(metadataRepository),
            writeTrackTagsToFile = WriteTrackTagsToFileUseCase(metadataRepository),
            mergeArtists = MergeArtistsUseCase(identityRepository),
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.open(TRACK_ID)
        advanceUntilIdle()
        return viewModel
    }

    private fun EditTrackMetadataViewModel.editing() =
        uiState.value as EditTrackMetadataUiState.Editing

    private fun track(artist: Artist = Artist("unknown", "Artista desconocido")) = Track(
        id = TRACK_ID,
        title = "Pista 1",
        artist = artist,
        durationMillis = 1_000L,
        mediaUri = "content://media/1",
        albumId = "album-unknown",
        albumTitle = "Álbum desconocido",
    )

    private fun override(title: String) = TrackMetadataOverride(
        trackId = TRACK_ID,
        fileKey = null,
        title = title,
        originalTitle = "Pista 1",
        originalArtistName = "Artista desconocido",
        originalAlbumTitle = "Álbum desconocido",
        durationMillis = 1_000L,
        updatedAtMillis = 1L,
    )

    private companion object {
        const val TRACK_ID = "track-1"
        val NOVA = Artist("artist-nova", "Nova")
        val ORIGINAL_ARTIST = Artist("artist-feat", "Nova feat. Luz")
    }
}

private class FakeTrackMetadataRepository : TrackMetadataRepository {
    val original = MutableStateFlow<Track?>(null)
    val overrides = MutableStateFlow(emptyList<TrackMetadataOverride>())
    var savedEdit: TrackMetadataEdit? = null
    var wroteFile = false

    override fun observeOverrides() = overrides
    override fun observeOriginalTrack(trackId: String) = original.map { it?.takeIf { track -> track.id == trackId } }
    override suspend fun saveEdit(trackId: String, edit: TrackMetadataEdit) {
        savedEdit = edit
    }
    override suspend fun reset(trackId: String) {
        overrides.value = emptyList()
    }
    override suspend fun writeTagsToFile(trackId: String) {
        wroteFile = true
    }
}

private class MetadataFakeArtistIdentityRepository : ArtistIdentityRepository {
    val aliases = MutableStateFlow(emptyList<ArtistAlias>())
    override fun observeAliases() = aliases
    override suspend fun getAliases() = aliases.value
    override suspend fun merge(source: Artist, target: Artist) {
        aliases.value += ArtistAlias(source, target)
    }
    override suspend fun unmerge(source: Artist) = Unit
    override suspend fun replaceAliases(aliases: List<ArtistAlias>) = Unit
    override suspend fun mergeAliases(aliases: List<ArtistAlias>) = 0
}

private class MetadataFakeLibraryRepository : LibraryRepository {
    val artists = MutableStateFlow(emptyList<ArtistSummary>())

    override fun observeAlbums() = MutableStateFlow(emptyList<Album>())
    override fun observeAlbumContent(albumId: String) = MutableStateFlow<AlbumContent?>(null)
    override fun observeArtists() = artists
    override fun observeArtistContent(artistId: String) = MutableStateFlow<ArtistContent?>(null)
    override fun observeTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeAllTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeFolders() = MutableStateFlow(emptyList<LibraryFolder>())
    override fun observeFolderContent(folderId: String) = MutableStateFlow<LibraryFolderContent?>(null)
    override suspend fun resolvePlaylistSource(source: PlaylistSource) = emptyList<Track>()
    override suspend fun refreshTracks() = 0
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}
