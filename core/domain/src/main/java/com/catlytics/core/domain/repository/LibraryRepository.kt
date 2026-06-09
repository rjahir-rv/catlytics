package com.catlytics.core.domain.repository

import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeTracks(): Flow<List<Track>>

    fun observeFolders(): Flow<List<LibraryFolder>>

    suspend fun refreshTracks()

    suspend fun setFolderVisible(folderId: String, visible: Boolean)
}
