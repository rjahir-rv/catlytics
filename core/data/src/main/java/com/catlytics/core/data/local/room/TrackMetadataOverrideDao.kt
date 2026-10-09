package com.catlytics.core.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TrackMetadataOverrideDao {
    @Query("SELECT * FROM track_metadata_overrides")
    abstract fun observeAll(): Flow<List<TrackMetadataOverrideEntity>>

    @Query("SELECT * FROM track_metadata_overrides")
    abstract suspend fun getAll(): List<TrackMetadataOverrideEntity>

    @Query("SELECT * FROM track_metadata_overrides WHERE track_id = :trackId")
    abstract suspend fun getByTrackId(trackId: String): TrackMetadataOverrideEntity?

    @Query("SELECT * FROM track_metadata_overrides WHERE file_key = :fileKey")
    abstract suspend fun getByFileKey(fileKey: String): TrackMetadataOverrideEntity?

    @Upsert
    abstract suspend fun upsert(override: TrackMetadataOverrideEntity)

    @Query("DELETE FROM track_metadata_overrides WHERE track_id = :trackId")
    abstract suspend fun delete(trackId: String)

    @Query("DELETE FROM track_metadata_overrides")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun upsertReplacingFileKey(override: TrackMetadataOverrideEntity) {
        override.fileKey
            ?.let { getByFileKey(it) }
            ?.takeIf { it.trackId != override.trackId }
            ?.let { delete(it.trackId) }
        upsert(override)
    }

    @Transaction
    open suspend fun replaceAll(overrides: List<TrackMetadataOverrideEntity>) {
        deleteAll()
        overrides.forEach { upsertReplacingFileKey(it) }
    }

    @Transaction
    open suspend fun moveTrackIds(moves: List<Pair<String, String>>) {
        moves.forEach { (oldTrackId, newTrackId) ->
            val existing = getByTrackId(oldTrackId) ?: return@forEach
            delete(oldTrackId)
            upsert(existing.copy(trackId = newTrackId))
        }
    }
}
