package com.catlytics.core.data.repository

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.model.toDomain
import com.catlytics.core.data.model.toEntity
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.Playlist
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstPlaylistRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
) : PlaylistRepository {
    override fun observePlaylists() = localDataSource.observePlaylists()
        .map { playlists -> playlists.map { it.toDomain() } }

    override suspend fun upsertPlaylist(playlist: Playlist) {
        localDataSource.upsertPlaylist(playlist.toEntity())
    }
}
