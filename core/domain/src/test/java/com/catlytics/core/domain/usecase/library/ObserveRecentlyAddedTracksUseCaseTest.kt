package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.HomeRecommendationsSettings
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveRecentlyAddedTracksUseCaseTest {
    private val nowMillis = 100 * DAY_MILLIS

    @Test
    fun `tracks within the window are returned newest first`() = runTest {
        val oldish = track(id = "track-1", addedAtMillis = nowMillis - 20 * DAY_MILLIS)
        val newer = track(id = "track-2", addedAtMillis = nowMillis - 2 * DAY_MILLIS)
        val newest = track(id = "track-3", addedAtMillis = nowMillis - DAY_MILLIS)
        val repository = FakeRecentlyAddedLibraryRepository(listOf(oldish, newer, newest))

        val result = useCase(repository).first()

        assertEquals(listOf("track-3", "track-2", "track-1"), result.map(Track::id))
    }

    @Test
    fun `tracks outside the twenty one day window are excluded`() = runTest {
        val recent = track(id = "track-1", addedAtMillis = nowMillis - 21 * DAY_MILLIS + 1)
        val expired = track(id = "track-2", addedAtMillis = nowMillis - 21 * DAY_MILLIS)
        val older = track(id = "track-3", addedAtMillis = nowMillis - 40 * DAY_MILLIS)
        val repository = FakeRecentlyAddedLibraryRepository(listOf(recent, expired, older))

        val result = useCase(repository).first()

        assertEquals(listOf("track-1"), result.map(Track::id))
    }

    @Test
    fun `tracks without an addition date are never recent`() = runTest {
        val repository = FakeRecentlyAddedLibraryRepository(
            listOf(
                track(id = "track-1", addedAtMillis = null),
                track(id = "track-2", addedAtMillis = nowMillis),
            ),
        )

        val result = useCase(repository).first()

        assertEquals(listOf("track-2"), result.map(Track::id))
    }

    @Test
    fun `empty library yields empty list`() = runTest {
        val repository = FakeRecentlyAddedLibraryRepository(emptyList())

        val result = useCase(repository).first()

        assertEquals(emptyList<Track>(), result)
    }

    @Test
    fun `recent window follows the configured day count`() = runTest {
        RecentAddedWindow.entries.forEach { window ->
            val inside = track(
                id = "track-1",
                addedAtMillis = nowMillis - (window.days - 1) * DAY_MILLIS,
            )
            val outside = track(
                id = "track-2",
                addedAtMillis = nowMillis - (window.days + 1) * DAY_MILLIS,
            )
            val repository = FakeRecentlyAddedLibraryRepository(listOf(inside, outside))
            val homePreferencesRepository = FakeHomePreferencesRepository(
                HomeRecommendationsSettings(recentAddedWindow = window),
            )

            val result = useCase(repository, homePreferencesRepository).first()

            assertEquals(
                "Ventana de ${window.days} días",
                listOf("track-1"),
                result.map(Track::id),
            )
        }
    }

    @Test
    fun `window changes are applied reactively`() = runTest {
        val inside = track(id = "track-1", addedAtMillis = nowMillis - 12 * DAY_MILLIS)
        val repository = FakeRecentlyAddedLibraryRepository(listOf(inside))
        val homePreferencesRepository = FakeHomePreferencesRepository()
        val tracks = useCase(repository, homePreferencesRepository)

        val before = tracks.first()
        homePreferencesRepository.setRecentAddedWindow(RecentAddedWindow.Days10)
        val after = tracks.first()

        assertEquals(listOf("track-1"), before.map(Track::id))
        assertEquals(emptyList<Track>(), after)
    }

    private fun useCase(
        repository: LibraryRepository,
        homePreferencesRepository: HomePreferencesRepository = FakeHomePreferencesRepository(),
    ): Flow<List<Track>> =
        ObserveRecentlyAddedTracksUseCase(
            libraryRepository = repository,
            homePreferencesRepository = homePreferencesRepository,
            nowMillis = { nowMillis },
        )()

    private fun track(id: String, addedAtMillis: Long?) = Track(
        id = id,
        title = "Track $id",
        artist = Artist(id = "artist-$id", name = "Artist $id"),
        durationMillis = 180_000L,
        addedAtMillis = addedAtMillis,
        mediaUri = "content://media/external/audio/media/$id",
    )

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

private class FakeRecentlyAddedLibraryRepository(
    initialTracks: List<Track>,
) : LibraryRepository {
    private val tracks = MutableStateFlow(initialTracks)

    override fun observeAlbums(): Flow<List<Album>> = flowOf(emptyList())
    override fun observeAlbumContent(albumId: String): Flow<AlbumContent?> = flowOf(null)
    override fun observeArtists(): Flow<List<ArtistSummary>> = flowOf(emptyList())
    override fun observeArtistContent(artistId: String): Flow<ArtistContent?> = flowOf(null)
    override fun observeTracks(): Flow<List<Track>> = tracks
    override fun observeAllTracks(): Flow<List<Track>> = tracks
    override fun observeFolders(): Flow<List<LibraryFolder>> = flowOf(emptyList())
    override fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?> = flowOf(null)
    override suspend fun resolvePlaylistSource(source: PlaylistSource): List<Track> = emptyList()
    override suspend fun refreshTracks(): Int = 0
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

private class FakeHomePreferencesRepository(
    initialSettings: HomeRecommendationsSettings = HomeRecommendationsSettings(),
) : HomePreferencesRepository {
    val settings = MutableStateFlow(initialSettings)

    override fun observeHomeRecommendationsSettings(): Flow<HomeRecommendationsSettings> = settings

    override suspend fun setShowRecommendedPlaylists(show: Boolean) {
        settings.update { it.copy(showRecommendedPlaylists = show) }
    }

    override suspend fun setRecentAddedWindow(window: RecentAddedWindow) {
        settings.update { it.copy(recentAddedWindow = window) }
    }

    override suspend fun setShowNewTrackBadge(show: Boolean) {
        settings.update { it.copy(showNewTrackBadge = show) }
    }
}
