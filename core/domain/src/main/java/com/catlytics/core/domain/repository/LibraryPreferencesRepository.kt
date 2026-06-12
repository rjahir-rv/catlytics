package com.catlytics.core.domain.repository

import com.catlytics.core.model.ArtistViewMode
import kotlinx.coroutines.flow.Flow

interface LibraryPreferencesRepository {
    fun observeHiddenFolderIds(): Flow<Set<String>>

    fun observeArtistViewMode(): Flow<ArtistViewMode>

    suspend fun setFolderVisible(folderId: String, visible: Boolean)

    suspend fun setArtistViewMode(viewMode: ArtistViewMode)
}
