package com.catlytics.core.domain.repository

import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import kotlinx.coroutines.flow.Flow

interface LibraryPreferencesRepository {
    fun observeHiddenFolderIds(): Flow<Set<String>>

    fun observeMusicScanSettings(): Flow<MusicScanSettings>

    fun observeArtistViewMode(): Flow<ArtistViewMode>

    fun observePlaylistViewMode(): Flow<PlaylistViewMode>

    fun observeLibrarySortDirection(): Flow<SortDirection>
    fun observePlaylistSortDirection(): Flow<SortDirection>

    suspend fun setFolderVisible(folderId: String, visible: Boolean)

    suspend fun setMusicScanDurationFilter(filter: MusicScanDurationFilter)

    suspend fun setMusicScanSizeFilter(filter: MusicScanSizeFilter)

    suspend fun setArtistViewMode(viewMode: ArtistViewMode)

    suspend fun setPlaylistViewMode(viewMode: PlaylistViewMode)

    suspend fun setLibrarySortDirection(direction: SortDirection)
    suspend fun setPlaylistSortDirection(direction: SortDirection)
}
