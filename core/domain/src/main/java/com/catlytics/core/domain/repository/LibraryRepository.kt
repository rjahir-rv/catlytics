package com.catlytics.core.domain.repository

import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeTracks(): Flow<List<Track>>

    fun observeAllTracks(): Flow<List<Track>>

    fun observeFolders(): Flow<List<LibraryFolder>>

    fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?>

    suspend fun refreshTracks()

    suspend fun setFolderVisible(folderId: String, visible: Boolean)
}
