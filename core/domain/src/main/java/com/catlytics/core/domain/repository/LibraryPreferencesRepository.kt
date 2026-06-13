package com.catlytics.core.domain.repository

import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.PlaylistViewMode
import kotlinx.coroutines.flow.Flow

interface LibraryPreferencesRepository {
    fun observeHiddenFolderIds(): Flow<Set<String>>

    fun observeArtistViewMode(): Flow<ArtistViewMode>

    fun observePlaylistViewMode(): Flow<PlaylistViewMode>

    suspend fun setFolderVisible(folderId: String, visible: Boolean)

    suspend fun setArtistViewMode(viewMode: ArtistViewMode)

    suspend fun setPlaylistViewMode(viewMode: PlaylistViewMode)
}
