package com.catlytics.feature.settings.impl.edits

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.TrackMetadataRepository
import com.catlytics.core.domain.usecase.library.ObserveArtistAliasesUseCase
import com.catlytics.core.domain.usecase.library.ObserveTrackMetadataOverridesUseCase
import com.catlytics.core.domain.usecase.library.ObserveTracksWithOverridesUseCase
import com.catlytics.core.domain.usecase.library.ResetTracksMetadataUseCase
import com.catlytics.core.domain.usecase.library.SaveBulkTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.UnmergeArtistUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.BulkEditResult
import com.catlytics.core.model.BulkTrackMetadataEdit
import com.catlytics.core.model.FieldEdit
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackMetadataEdit
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.core.model.renamedArtistId
import com.catlytics.feature.settings.impl.MainDispatcherRule
import com.catlytics.feature.settings.impl.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetadataEditsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val metadataRepository = EditsFakeTrackMetadataRepository()
    private val libraryRepository = EditsFakeLibraryRepository()
    private val identityRepository = EditsFakeArtistIdentityRepository()

    @Test
    fun `lists track edits newest first with their changes and missing tracks`() = runTest {
        libraryRepository.allTracks.value = listOf(track("1", "Editada", "Nova"))
        metadataRepository.overrides.value = listOf(
            override("1", title = "Editada", artist = "Nova", updatedAt = 1L),
            override("gone", album = "Debut", updatedAt = 2L),
        )
        val viewModel = openedViewModel()

        val state = viewModel.uiState.value
        assertEquals(listOf("gone", "1"), state.tracks.map { it.trackId })
        val edited = state.tracks.single { it.trackId == "1" }
        assertTrue(edited.inLibrary)
        assertEquals(
            listOf(
                FieldChange(EditedField.Title, "Pista 1", "Editada"),
                FieldChange(EditedField.Artist, "Original", "Nova"),
            ),
            edited.changes,
        )
        val missing = state.tracks.single { it.trackId == "gone" }
        assertFalse(missing.inLibrary)
        assertEquals("Pista gone", missing.title)
    }

    @Test
    fun `filters and search narrow tracks and artists`() = runTest {
        libraryRepository.allTracks.value = listOf(track("1"), track("2"))
        metadataRepository.overrides.value = listOf(
            override("1", title = "Intro"),
            override("2", artwork = "/files/track_artwork/a.cover"),
        )
        identityRepository.aliases.value = listOf(
            ArtistAlias(Artist("a", "beatles"), Artist("a", "Beatles")),
            ArtistAlias(Artist("b", "Felix"), Artist("c", "Post Malone")),
            ArtistAlias(Artist("d", "Viejo"), Artist(renamedArtistId("Nuevo"), "Nuevo")),
        )
        val viewModel = openedViewModel()
        assertEquals(
            listOf(ArtistEditKind.Spelling, ArtistEditKind.Merge, ArtistEditKind.Rename),
            viewModel.uiState.value.artists.map { it.kind },
        )

        viewModel.onFilterChange(MetadataEditFilter.Artwork)
        advanceUntilIdle()
        assertEquals(listOf("2"), viewModel.uiState.value.tracks.map { it.trackId })
        assertTrue(viewModel.uiState.value.artists.isEmpty())

        viewModel.onFilterChange(MetadataEditFilter.All)
        viewModel.onQueryChange("post")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.tracks.isEmpty())
        assertEquals(listOf("Felix"), viewModel.uiState.value.artists.map { it.alias.source.name })
        assertEquals(2, viewModel.uiState.value.totalTrackEdits)
    }

    @Test
    fun `reverting a field restores only that field`() = runTest {
        libraryRepository.allTracks.value = listOf(track("1"))
        metadataRepository.overrides.value = listOf(override("1", title = "Intro", album = "Debut"))
        val viewModel = openedViewModel()
        val messages = collectMessages(viewModel)

        viewModel.revertField("1", EditedField.Album)
        advanceUntilIdle()

        assertEquals(listOf("1") to BulkTrackMetadataEdit(albumTitle = FieldEdit.Restore), metadataRepository.bulkEdit)
        assertEquals(listOf(UiText.Resource(R.string.settings_edits_field_reverted)), messages)
    }

    @Test
    fun `reverting the selection resets those tracks after confirming`() = runTest {
        libraryRepository.allTracks.value = listOf(track("1"), track("2"), track("3"))
        metadataRepository.overrides.value = listOf(
            override("1", title = "A"),
            override("2", title = "B"),
            override("3", title = "C"),
        )
        val viewModel = openedViewModel()

        viewModel.toggleSelection("1")
        viewModel.toggleSelection("3")
        advanceUntilIdle()
        viewModel.requestRevertSelected()
        advanceUntilIdle()
        assertEquals(MetadataEditsDialog.ConfirmRevertSelected(2), viewModel.uiState.value.dialog)

        viewModel.confirmDialog()
        advanceUntilIdle()

        assertEquals(listOf("2"), metadataRepository.overrides.value.map { it.trackId })
        assertTrue(viewModel.uiState.value.selectedTrackIds.isEmpty())
        assertEquals(MetadataEditsDialog.Hidden, viewModel.uiState.value.dialog)
    }

    @Test
    fun `reverting everything filtered includes the visible artist edits`() = runTest {
        libraryRepository.allTracks.value = listOf(track("1"))
        metadataRepository.overrides.value = listOf(override("1", artist = "Nova"), override("gone", title = "X"))
        identityRepository.aliases.value = listOf(ArtistAlias(Artist("b", "Felix"), Artist("c", "Post Malone")))
        val viewModel = openedViewModel()

        viewModel.onFilterChange(MetadataEditFilter.Missing)
        advanceUntilIdle()
        viewModel.requestRevertFiltered()
        advanceUntilIdle()
        assertEquals(MetadataEditsDialog.ConfirmRevertFiltered(1, 0), viewModel.uiState.value.dialog)
        viewModel.confirmDialog()
        advanceUntilIdle()
        // Only the filtered (missing) edit is reverted.
        assertEquals(listOf("1"), metadataRepository.overrides.value.map { it.trackId })
        assertEquals(1, identityRepository.aliases.value.size)

        viewModel.onFilterChange(MetadataEditFilter.All)
        advanceUntilIdle()
        viewModel.requestRevertFiltered()
        viewModel.confirmDialog()
        advanceUntilIdle()

        assertTrue(metadataRepository.overrides.value.isEmpty())
        assertTrue(identityRepository.aliases.value.isEmpty())
        assertFalse(viewModel.uiState.value.hasAnyEdits)
    }

    @Test
    fun `reverting an artist edit undoes the merge`() = runTest {
        val alias = ArtistAlias(Artist("b", "Felix"), Artist("c", "Post Malone"))
        identityRepository.aliases.value = listOf(alias)
        val viewModel = openedViewModel()
        val messages = collectMessages(viewModel)

        viewModel.revertArtist(alias)
        advanceUntilIdle()

        assertTrue(identityRepository.aliases.value.isEmpty())
        assertEquals(listOf(UiText.Resource(R.string.settings_edits_artist_reverted, listOf("Felix"))), messages)
    }

    private fun TestScope.openedViewModel(): MetadataEditsViewModel {
        val viewModel = MetadataEditsViewModel(
            observeTrackMetadataOverrides = ObserveTrackMetadataOverridesUseCase(metadataRepository),
            observeTracksWithOverrides = ObserveTracksWithOverridesUseCase(metadataRepository, libraryRepository),
            observeArtistAliases = ObserveArtistAliasesUseCase(identityRepository),
            saveBulkTrackMetadata = SaveBulkTrackMetadataUseCase(metadataRepository),
            resetTracksMetadata = ResetTracksMetadataUseCase(metadataRepository),
            unmergeArtist = UnmergeArtistUseCase(identityRepository),
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.collectMessages(viewModel: MetadataEditsViewModel): List<UiText> {
        val messages = mutableListOf<UiText>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.messages.collect(messages::add)
        }
        return messages
    }

    private fun track(id: String, title: String = "Pista $id", artist: String = "Original") = Track(
        id = id,
        title = title,
        artist = Artist("artist-$artist", artist),
        durationMillis = 1_000L,
        mediaUri = "content://media/$id",
    )

    private fun override(
        id: String,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        artwork: String? = null,
        updatedAt: Long = 1L,
    ) = TrackMetadataOverride(
        trackId = id,
        fileKey = null,
        title = title,
        artistName = artist,
        albumTitle = album,
        artworkUri = artwork,
        originalTitle = "Pista $id",
        originalArtistName = "Original",
        originalAlbumTitle = "Álbum",
        durationMillis = 1_000L,
        updatedAtMillis = updatedAt,
    )
}

private class EditsFakeTrackMetadataRepository : TrackMetadataRepository {
    val overrides = MutableStateFlow(emptyList<TrackMetadataOverride>())
    var bulkEdit: Pair<List<String>, BulkTrackMetadataEdit>? = null

    override fun observeOverrides() = overrides
    override fun observeOriginalTrack(trackId: String) = flowOf<Track?>(null)
    override suspend fun saveEdit(trackId: String, edit: TrackMetadataEdit) = Unit
    override suspend fun saveBulkEdit(trackIds: List<String>, edit: BulkTrackMetadataEdit): BulkEditResult {
        bulkEdit = trackIds to edit
        return BulkEditResult(updated = trackIds.size, skipped = 0)
    }
    override suspend fun reset(trackId: String) = resetMany(listOf(trackId))
    override suspend fun resetMany(trackIds: List<String>) {
        overrides.value = overrides.value.filterNot { it.trackId in trackIds }
    }
    override suspend fun writeTagsToFile(trackId: String) = Unit
    override suspend fun writeTagsToFiles(trackIds: List<String>) = 0
}

private class EditsFakeArtistIdentityRepository : ArtistIdentityRepository {
    val aliases = MutableStateFlow(emptyList<ArtistAlias>())

    override fun observeAliases() = aliases
    override suspend fun getAliases() = aliases.value
    override suspend fun merge(source: Artist, target: Artist) = Unit
    override suspend fun rename(source: Artist, newName: String) = Unit
    override suspend fun unmerge(source: Artist) {
        aliases.value = aliases.value.filterNot { it.source.name == source.name }
    }
    override suspend fun replaceAliases(aliases: List<ArtistAlias>) = Unit
    override suspend fun mergeAliases(aliases: List<ArtistAlias>) = 0
}

private class EditsFakeLibraryRepository : LibraryRepository {
    val allTracks = MutableStateFlow(emptyList<Track>())

    override fun observeAlbums() = MutableStateFlow(emptyList<Album>())
    override fun observeAlbumContent(albumId: String) = MutableStateFlow<AlbumContent?>(null)
    override fun observeArtists() = MutableStateFlow(emptyList<ArtistSummary>())
    override fun observeArtistContent(artistId: String) = MutableStateFlow<ArtistContent?>(null)
    override fun observeTracks() = allTracks
    override fun observeAllTracks() = allTracks
    override fun observeFolders() = MutableStateFlow(emptyList<LibraryFolder>())
    override fun observeFolderContent(folderId: String) = MutableStateFlow<LibraryFolderContent?>(null)
    override suspend fun resolvePlaylistSource(source: PlaylistSource) = emptyList<Track>()
    override suspend fun refreshTracks() = 0
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}
