package com.catlytics.core.domain.repository

import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeAlbums(): Flow<List<Album>>

    fun observeAlbumContent(albumId: String): Flow<AlbumContent?>

    fun observeArtists(): Flow<List<ArtistSummary>>

    fun observeArtistContent(artistId: String): Flow<ArtistContent?>

    fun observeTracks(): Flow<List<Track>>

    fun observeAllTracks(): Flow<List<Track>>

    fun observeFolders(): Flow<List<LibraryFolder>>

    fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?>

    suspend fun refreshTracks()

    suspend fun setFolderVisible(folderId: String, visible: Boolean)
}
