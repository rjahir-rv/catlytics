package com.catlytics.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.AlbumTrackSort
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.DetailSortSettings
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

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

    override fun observeHiddenFolderIds(): Flow<Set<String>> = dataStore.observe { preferences ->
        preferences[HIDDEN_FOLDER_IDS].orEmpty()
    }

    override fun observeMusicScanSettings(): Flow<MusicScanSettings> = dataStore.observe { preferences ->
        MusicScanSettings(
            durationFilter = preferences[MUSIC_SCAN_DURATION_FILTER].toEnumOr(MusicScanDurationFilter.Disabled),
            sizeFilter = preferences[MUSIC_SCAN_SIZE_FILTER].toEnumOr(MusicScanSizeFilter.Disabled),
        )
    }

    override fun observeArtistViewMode(): Flow<ArtistViewMode> = dataStore.observe { preferences ->
        preferences[ARTIST_VIEW_MODE].toEnumOr(ArtistViewMode.List)
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

    override suspend fun setMusicScanDurationFilter(filter: MusicScanDurationFilter) {
        dataStore.edit { preferences ->
            preferences[MUSIC_SCAN_DURATION_FILTER] = filter.name
        }
    }

    override suspend fun setMusicScanSizeFilter(filter: MusicScanSizeFilter) {
        dataStore.edit { preferences ->
            preferences[MUSIC_SCAN_SIZE_FILTER] = filter.name
        }
    }

    override suspend fun setArtistViewMode(viewMode: ArtistViewMode) {
        dataStore.edit { preferences ->
            preferences[ARTIST_VIEW_MODE] = viewMode.name
        }
    }

    override fun observePlaylistViewMode(): Flow<PlaylistViewMode> = dataStore.observe { preferences ->
        preferences[PLAYLIST_VIEW_MODE].toEnumOr(PlaylistViewMode.List)
    }

    override fun observeLibrarySortDirection(): Flow<SortDirection> = dataStore.observe { preferences ->
        preferences[LIBRARY_SORT_DIRECTION].toEnumOr(SortDirection.Ascending)
    }

    override fun observePlaylistSortDirection(): Flow<SortDirection> = dataStore.observe { preferences ->
        preferences[PLAYLIST_SORT_DIRECTION].toEnumOr(SortDirection.Ascending)
    }

    override fun observeDetailSortSettings(): Flow<DetailSortSettings> = dataStore.observe { preferences ->
        val defaults = DetailSortSettings()
        DetailSortSettings(
            albumTracks = preferences[ALBUM_TRACK_SORT].toEnumOr(defaults.albumTracks),
            artistTracks = preferences[ARTIST_TRACK_SORT].toEnumOr(defaults.artistTracks),
            artistAlbums = preferences[ARTIST_ALBUM_SORT].toEnumOr(defaults.artistAlbums),
        )
    }

    override suspend fun setAlbumTrackSort(sort: AlbumTrackSort) {
        dataStore.edit { preferences -> preferences[ALBUM_TRACK_SORT] = sort.name }
    }

    override suspend fun setArtistTrackSort(sort: ArtistTrackSort) {
        dataStore.edit { preferences -> preferences[ARTIST_TRACK_SORT] = sort.name }
    }

    override suspend fun setArtistAlbumSort(sort: ArtistAlbumSort) {
        dataStore.edit { preferences -> preferences[ARTIST_ALBUM_SORT] = sort.name }
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
        val MUSIC_SCAN_DURATION_FILTER = stringPreferencesKey("music_scan_duration_filter")
        val MUSIC_SCAN_SIZE_FILTER = stringPreferencesKey("music_scan_size_filter")
        val ARTIST_VIEW_MODE = stringPreferencesKey("artist_view_mode")
        val PLAYLIST_VIEW_MODE = stringPreferencesKey("playlist_view_mode")
        val LIBRARY_SORT_DIRECTION = stringPreferencesKey("library_sort_direction")
        val PLAYLIST_SORT_DIRECTION = stringPreferencesKey("playlist_sort_direction")
        val ALBUM_TRACK_SORT = stringPreferencesKey("album_track_sort")
        val ARTIST_TRACK_SORT = stringPreferencesKey("artist_track_sort")
        val ARTIST_ALBUM_SORT = stringPreferencesKey("artist_album_sort")
    }
}
