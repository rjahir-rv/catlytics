package com.catlytics.core.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ArtistArtworkOverrideDao {
    @Query("SELECT * FROM artist_artwork_overrides ORDER BY artist_name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<ArtistArtworkOverrideEntity>>

    @Query("SELECT * FROM artist_artwork_overrides")
    abstract suspend fun getAll(): List<ArtistArtworkOverrideEntity>

    @Query("SELECT * FROM artist_artwork_overrides WHERE artist_key = :artistKey")
    abstract suspend fun getByKey(artistKey: String): ArtistArtworkOverrideEntity?

    @Query("SELECT * FROM artist_artwork_overrides WHERE artist_key IN (:artistKeys)")
    abstract suspend fun getByKeys(artistKeys: List<String>): List<ArtistArtworkOverrideEntity>

    @Upsert
    abstract suspend fun upsert(override: ArtistArtworkOverrideEntity)

    @Query("DELETE FROM artist_artwork_overrides WHERE artist_key IN (:artistKeys)")
    abstract suspend fun deleteByKeys(artistKeys: List<String>)

    @Query("DELETE FROM artist_artwork_overrides")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(overrides: List<ArtistArtworkOverrideEntity>) {
        deleteAll()
        overrides.forEach { upsert(it) }
    }
}
