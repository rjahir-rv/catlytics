package com.catlytics.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.libraryPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "library_preferences",
)

@Singleton
class DataStoreLibraryPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : LibraryPreferencesRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.libraryPreferencesDataStore)

    override fun observeHiddenFolderIds(): Flow<Set<String>> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences[HIDDEN_FOLDER_IDS].orEmpty() }

    override fun observeArtistViewMode(): Flow<ArtistViewMode> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[ARTIST_VIEW_MODE]
                ?.let { stored -> ArtistViewMode.entries.firstOrNull { it.name == stored } }
                ?: ArtistViewMode.List
        }

    override suspend fun setFolderVisible(folderId: String, visible: Boolean) {
        dataStore.edit { preferences ->
            val hiddenFolderIds = preferences[HIDDEN_FOLDER_IDS].orEmpty()
            preferences[HIDDEN_FOLDER_IDS] = if (visible) {
                hiddenFolderIds - folderId
            } else {
                hiddenFolderIds + folderId
            }
        }
    }

    override suspend fun setArtistViewMode(viewMode: ArtistViewMode) {
        dataStore.edit { preferences ->
            preferences[ARTIST_VIEW_MODE] = viewMode.name
        }
    }

    override fun observePlaylistViewMode(): Flow<PlaylistViewMode> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[PLAYLIST_VIEW_MODE]
                ?.let { stored -> PlaylistViewMode.entries.firstOrNull { it.name == stored } }
                ?: PlaylistViewMode.List
        }

    override fun observeLibrarySortDirection(): Flow<SortDirection> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[LIBRARY_SORT_DIRECTION]
                ?.let { stored -> SortDirection.entries.firstOrNull { it.name == stored } }
                ?: SortDirection.Ascending
        }

    override fun observePlaylistSortDirection(): Flow<SortDirection> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[PLAYLIST_SORT_DIRECTION]
                ?.let { stored -> SortDirection.entries.firstOrNull { it.name == stored } }
                ?: SortDirection.Ascending
        }

    override suspend fun setPlaylistViewMode(viewMode: PlaylistViewMode) {
        dataStore.edit { preferences ->
            preferences[PLAYLIST_VIEW_MODE] = viewMode.name
        }
    }

    override suspend fun setLibrarySortDirection(direction: SortDirection) {
        dataStore.edit { preferences ->
            preferences[LIBRARY_SORT_DIRECTION] = direction.name
        }
    }

    override suspend fun setPlaylistSortDirection(direction: SortDirection) {
        dataStore.edit { preferences ->
            preferences[PLAYLIST_SORT_DIRECTION] = direction.name
        }
    }

    private companion object {
        val HIDDEN_FOLDER_IDS = stringSetPreferencesKey("hidden_folder_ids")
        val ARTIST_VIEW_MODE = stringPreferencesKey("artist_view_mode")
        val PLAYLIST_VIEW_MODE = stringPreferencesKey("playlist_view_mode")
        val LIBRARY_SORT_DIRECTION = stringPreferencesKey("library_sort_direction")
        val PLAYLIST_SORT_DIRECTION = stringPreferencesKey("playlist_sort_direction")
    }
}
