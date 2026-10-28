package com.catlytics.feature.library.impl.artist

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.repository.ArtistArtworkRepository
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.usecase.library.MergeArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistAliasesUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistContentUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryTrackUseCase
import com.catlytics.core.domain.usecase.library.RenameArtistUseCase
import com.catlytics.core.domain.usecase.library.RestoreArtistArtworkUseCase
import com.catlytics.core.domain.usecase.library.SetArtistArtworkUseCase
import com.catlytics.core.domain.usecase.library.UnmergeArtistUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtistArtworkOverride
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import com.catlytics.feature.library.impl.R
import com.catlytics.feature.library.impl.collection.SortTestFakes
import com.catlytics.feature.library.impl.root.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryArtistViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sort = SortTestFakes()

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

    @Test
    fun `searching filters songs and albums by query`() = runTest {
        val repository = ArtistFakeLibraryRepository()
        val trackOne = track("one").copy(title = "Bug Like an Angel")
        val trackTwo = track("two").copy(title = "Heaven")
        val albumOne = Album("album-1", "Laurel Hell", Artist(ARTIST_ID, "Artist"), trackCount = 1)
        val albumTwo = Album("album-2", "Be the Cowboy", Artist(ARTIST_ID, "Artist"), trackCount = 1)
        val content = ArtistContent(
            summary = ArtistSummary(Artist(ARTIST_ID, "Artist"), albumCount = 2, trackCount = 2),
            albums = listOf(albumOne, albumTwo),
            tracks = listOf(trackOne, trackTwo),
        )
        repository.content.value = content
        val viewModel = viewModel(repository, ArtistFakePlaybackController())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.openArtist(ARTIST_ID)
        advanceUntilIdle()

        // Search for track title fragment
        viewModel.onSearchQueryChanged("bug")
        advanceUntilIdle()

        val expectedContent = ArtistContent(
            summary = ArtistSummary(Artist(ARTIST_ID, "Artist"), albumCount = 0, trackCount = 1),
            albums = emptyList(),
            tracks = listOf(trackOne),
        )
        assertEquals(
            LibraryArtistUiState.Success(
                content = expectedContent,
                searchQuery = "bug",
                playbackQueue = listOf(trackOne, trackTwo),
            ),
            viewModel.uiState.value,
        )

        // Search for album title fragment
        viewModel.onSearchQueryChanged("laurel")
        advanceUntilIdle()

        val expectedContentAlbum = ArtistContent(
            summary = ArtistSummary(Artist(ARTIST_ID, "Artist"), albumCount = 1, trackCount = 0),
            albums = listOf(albumOne),
            tracks = emptyList(),
        )
        assertEquals(
            LibraryArtistUiState.Success(
                content = expectedContentAlbum,
                searchQuery = "laurel",
                playbackQueue = listOf(trackOne, trackTwo),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `confirming merge sends current artist to selected principal`() = runTest {
        val repository = ArtistFakeLibraryRepository()
        val current = artistContent()
        val target = ArtistSummary(Artist("artist-main", "Artista principal"), albumCount = 1, trackCount = 1)
        repository.content.value = current
        repository.artists.value = listOf(current.summary, target)
        val identityRepository = FakeArtistIdentityRepository()
        val viewModel = viewModel(
            repository,
            ArtistFakePlaybackController(),
            identityRepository,
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.openArtist(ARTIST_ID)
        advanceUntilIdle()

        viewModel.showMergePicker()
        viewModel.selectMergeTarget(target.artist)
        viewModel.confirmMerge()
        advanceUntilIdle()

        assertEquals(current.summary.artist, identityRepository.getAliases().single().source)
        assertEquals(target.artist, identityRepository.getAliases().single().target)
    }

    @Test
    fun `renaming to a new name renames the artist`() = runTest {
        val repository = ArtistFakeLibraryRepository()
        repository.content.value = artistContent()
        val identityRepository = FakeArtistIdentityRepository()
        val viewModel = viewModel(repository, ArtistFakePlaybackController(), identityRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.openArtist(ARTIST_ID)
        advanceUntilIdle()

        viewModel.showRename()
        viewModel.updateRenameName(" Nuevo Nombre ")
        advanceUntilIdle()
        viewModel.confirmRename()
        advanceUntilIdle()

        assertEquals(Artist(ARTIST_ID, "Artist") to "Nuevo Nombre", identityRepository.renamed)
        assertEquals(ArtistMergeDialog.Hidden, (viewModel.uiState.value as LibraryArtistUiState.Success).mergeDialog)
    }

    @Test
    fun `changing and restoring the image apply to the current artist`() = runTest {
        val repository = ArtistFakeLibraryRepository()
        repository.content.value = artistContent()
        val artworkRepository = FakeArtistArtworkRepository()
        val viewModel = viewModel(
            repository,
            ArtistFakePlaybackController(),
            artworkRepository = artworkRepository,
        )
        val messages = mutableListOf<UiText>()
        backgroundScope.launch { viewModel.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.messages.collect(messages::add) }
        viewModel.openArtist(ARTIST_ID)
        advanceUntilIdle()

        viewModel.changeArtwork("content://picker/image")
        advanceUntilIdle()
        viewModel.restoreArtwork()
        advanceUntilIdle()

        assertEquals(Artist(ARTIST_ID, "Artist") to "content://picker/image", artworkRepository.changed)
        assertEquals(Artist(ARTIST_ID, "Artist"), artworkRepository.restored)
        assertEquals(
            listOf(
                UiText.Resource(R.string.library_artist_image_changed),
                UiText.Resource(R.string.library_artist_image_restored),
            ),
            messages,
        )
    }

    @Test
    fun `renaming to an existing artist asks to merge instead`() = runTest {
        val repository = ArtistFakeLibraryRepository()
        repository.content.value = artistContent()
        val existing = Artist("artist-nova", "Nova")
        repository.artists.value = listOf(ArtistSummary(existing, albumCount = 1, trackCount = 1))
        val identityRepository = FakeArtistIdentityRepository()
        val viewModel = viewModel(repository, ArtistFakePlaybackController(), identityRepository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.openArtist(ARTIST_ID)
        advanceUntilIdle()

        viewModel.showRename()
        viewModel.updateRenameName("nova")
        advanceUntilIdle()
        assertEquals(existing, (viewModel.uiState.value as LibraryArtistUiState.Success).renameMergeTarget)
        viewModel.confirmRename()
        advanceUntilIdle()

        assertEquals(null, identityRepository.renamed)
        assertEquals(
            ArtistMergeDialog.ConfirmMerge(existing),
            (viewModel.uiState.value as LibraryArtistUiState.Success).mergeDialog,
        )
    }

    @Test
    fun `an artist that gets a new id is followed through its tracks`() = runTest {
        val repository = ArtistFakeLibraryRepository()
        val renamed = Artist("renamed-artist-nuevo", "Nuevo")
        val renamedContent = artistContent().let { content ->
            content.copy(
                summary = content.summary.copy(artist = renamed),
                tracks = content.tracks.map { it.copy(artist = renamed) },
            )
        }
        repository.contentById[ARTIST_ID] = MutableStateFlow(artistContent())
        repository.contentById[renamed.id] = MutableStateFlow(renamedContent)
        val viewModel = viewModel(repository, ArtistFakePlaybackController())
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.openArtist(ARTIST_ID)
        advanceUntilIdle()

        repository.allTracks.value = renamedContent.tracks
        repository.contentById.getValue(ARTIST_ID).value = null
        advanceUntilIdle()

        assertEquals(
            renamed,
            (viewModel.uiState.value as LibraryArtistUiState.Success).content.summary.artist,
        )
    }

    @Test
    fun `artist tracks and albums follow the remembered orders and the queue too`() = runTest {
        val repository = ArtistFakeLibraryRepository()
        val tracks = listOf(track("one").copy(title = "Zeta"), track("two").copy(title = "alfa"))
        val albums = listOf(
            Album("a", "Beta", Artist(ARTIST_ID, "Artist"), trackCount = 1),
            Album("b", "Alfa", Artist(ARTIST_ID, "Artist"), trackCount = 3),
        )
        repository.content.value = artistContent().copy(tracks = tracks, albums = albums)
        val viewModel = viewModel(repository, ArtistFakePlaybackController())
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.openArtist(ARTIST_ID)
        advanceUntilIdle()

        viewModel.setTrackSort(ArtistTrackSort.Title)
        viewModel.setAlbumSort(ArtistAlbumSort.TrackCount)
        advanceUntilIdle()

        val state = viewModel.uiState.value as LibraryArtistUiState.Success
        assertEquals(listOf("two", "one"), state.content.tracks.map { it.id })
        assertEquals(listOf("two", "one"), state.playbackQueue.map { it.id })
        assertEquals(listOf("b", "a"), state.content.albums.map { it.id })
        assertEquals(ArtistTrackSort.Title, state.trackSort)
    }

    @Test
    fun `shuffling the artist plays every track`() = runTest {
        val playbackController = ArtistFakePlaybackController()
        val viewModel = viewModel(ArtistFakeLibraryRepository(), playbackController)
        val queue = listOf(track("one"), track("two"), track("three"))

        viewModel.playShuffled(queue)
        advanceUntilIdle()

        assertEquals(queue.toSet(), playbackController.playedQueue.toSet())
    }

    private fun viewModel(
        repository: ArtistFakeLibraryRepository,
        playbackController: ArtistFakePlaybackController,
        identityRepository: FakeArtistIdentityRepository = FakeArtistIdentityRepository(),
        artworkRepository: FakeArtistArtworkRepository = FakeArtistArtworkRepository(),
    ) = LibraryArtistViewModel(
        observeArtistContentUseCase = ObserveArtistContentUseCase(repository),
        observeArtistsUseCase = ObserveArtistsUseCase(repository),
        observeArtistAliasesUseCase = ObserveArtistAliasesUseCase(identityRepository),
        mergeArtistsUseCase = MergeArtistsUseCase(identityRepository),
        unmergeArtistUseCase = UnmergeArtistUseCase(identityRepository),
        playTrackUseCase = PlayTrackUseCase(playbackController),
        renameArtistUseCase = RenameArtistUseCase(identityRepository),
        observeLibraryTrack = ObserveLibraryTrackUseCase(repository),
        playShuffledQueueUseCase = PlayShuffledQueueUseCase(playbackController),
        observeDetailSortSettings = sort.observeSettings,
        observeTrackPlayCounts = sort.observePlayCounts,
        setArtistTrackSort = sort.setArtistTrackSort,
        setArtistAlbumSort = sort.setArtistAlbumSort,
        setArtistArtwork = SetArtistArtworkUseCase(artworkRepository),
        restoreArtistArtwork = RestoreArtistArtworkUseCase(artworkRepository),
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

private class FakeArtistArtworkRepository : ArtistArtworkRepository {
    var changed: Pair<Artist, String>? = null
    var restored: Artist? = null
    override fun observeOverrides() = MutableStateFlow(emptyList<ArtistArtworkOverride>())
    override suspend fun setArtwork(artist: Artist, sourceUri: String) {
        changed = artist to sourceUri
    }
    override suspend fun restoreArtwork(artist: Artist) {
        restored = artist
    }
    override suspend fun removeArtwork(artistKey: String) = Unit
}

private class FakeArtistIdentityRepository : ArtistIdentityRepository {
    var renamed: Pair<Artist, String>? = null
    override suspend fun rename(source: Artist, newName: String) {
        renamed = source to newName
    }
    private val aliases = MutableStateFlow(emptyList<ArtistAlias>())
    override fun observeAliases() = aliases
    override suspend fun getAliases() = aliases.value
    override suspend fun merge(source: Artist, target: Artist) {
        aliases.value += ArtistAlias(source, target)
    }
    override suspend fun unmerge(source: Artist) {
        aliases.value = aliases.value.filterNot { it.source == source }
    }
    override suspend fun replaceAliases(aliases: List<ArtistAlias>) {
        this.aliases.value = aliases
    }
    override suspend fun mergeAliases(aliases: List<ArtistAlias>): Int {
        this.aliases.value += aliases
        return aliases.size
    }
}

private class ArtistFakeLibraryRepository : LibraryRepository {
    val content = MutableStateFlow<ArtistContent?>(null)
    val contentById = mutableMapOf<String, MutableStateFlow<ArtistContent?>>()
    val artists = MutableStateFlow(emptyList<ArtistSummary>())
    val allTracks = MutableStateFlow(emptyList<Track>())

    override fun observeAlbums() = MutableStateFlow(emptyList<Album>())
    override fun observeAlbumContent(albumId: String) = MutableStateFlow<AlbumContent?>(null)
    override fun observeArtists() = artists
    override fun observeArtistContent(artistId: String) = contentById[artistId] ?: content
    override fun observeTracks() = MutableStateFlow(emptyList<Track>())
    override fun observeAllTracks() = allTracks
    override fun observeFolders() = MutableStateFlow(emptyList<LibraryFolder>())
    override fun observeFolderContent(folderId: String) =
        MutableStateFlow<LibraryFolderContent?>(null)
    override suspend fun resolvePlaylistSource(source: com.catlytics.core.model.PlaylistSource) =
        emptyList<Track>()
    override suspend fun refreshTracks() = 0
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

private class ArtistFakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = MutableStateFlow(PlaybackState())
    lateinit var playedTrack: Track
    lateinit var playedQueue: List<Track>
    var startIndex = -1

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) {
        playedTrack = track
        playedQueue = queue
        this.startIndex = startIndex
    }

    override suspend fun playQueueItem(index: Int) = Unit
    override suspend fun addQueueItem(track: Track) = Unit

    override suspend fun playNext(track: Track) = Unit
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
