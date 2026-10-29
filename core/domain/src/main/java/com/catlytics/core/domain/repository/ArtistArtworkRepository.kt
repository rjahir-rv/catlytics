package com.catlytics.core.domain.repository

import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistArtworkOverride
import kotlinx.coroutines.flow.Flow

interface ArtistArtworkRepository {
    fun observeOverrides(): Flow<List<ArtistArtworkOverride>>

    suspend fun setArtwork(artist: Artist, sourceUri: String)

    suspend fun restoreArtwork(artist: Artist)

    suspend fun removeArtwork(artistKey: String)
}
