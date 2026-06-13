package com.catlytics.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.PlaylistViewMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreLibraryPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `folders are visible by default`() = runTest {
        val repository = repository(backgroundScope)

        assertEquals(emptySet<String>(), repository.observeHiddenFolderIds().first())
    }

    @Test
    fun `hidden folder remains persisted until shown again`() = runTest {
        val repository = repository(backgroundScope)

        repository.setFolderVisible(FOLDER_ID, visible = false)
        assertEquals(setOf(FOLDER_ID), repository.observeHiddenFolderIds().first())

        repository.setFolderVisible(FOLDER_ID, visible = true)
        assertEquals(emptySet<String>(), repository.observeHiddenFolderIds().first())
    }

    @Test
    fun `artist view mode defaults to list and persists changes`() = runTest {
        val repository = repository(backgroundScope)

        assertEquals(ArtistViewMode.List, repository.observeArtistViewMode().first())

        repository.setArtistViewMode(ArtistViewMode.Grid)

        assertEquals(ArtistViewMode.Grid, repository.observeArtistViewMode().first())
    }

    @Test
    fun `playlist view mode defaults to list and persists changes`() = runTest {
        val repository = repository(backgroundScope)

        assertEquals(PlaylistViewMode.List, repository.observePlaylistViewMode().first())

        repository.setPlaylistViewMode(PlaylistViewMode.Mosaic)

        assertEquals(PlaylistViewMode.Mosaic, repository.observePlaylistViewMode().first())
    }

    private fun repository(scope: CoroutineScope): LibraryPreferencesRepository =
        DataStoreLibraryPreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = {
                    temporaryFolder.newFile("library-${System.nanoTime()}.preferences_pb")
                },
            ),
        )

    private companion object {
        const val FOLDER_ID = "external_primary:Music/Game"
    }
}
