package com.catlytics.core.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ArtistAliasDao {
    @Query("SELECT * FROM artist_aliases ORDER BY source_artist_name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<ArtistAliasEntity>>

    @Query("SELECT * FROM artist_aliases ORDER BY source_artist_name COLLATE NOCASE")
    abstract suspend fun getAll(): List<ArtistAliasEntity>

    @Query("SELECT * FROM artist_aliases WHERE source_key = :sourceKey")
    abstract suspend fun getBySourceKey(sourceKey: String): ArtistAliasEntity?

    @Upsert
    abstract suspend fun upsert(alias: ArtistAliasEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(alias: ArtistAliasEntity): Long

    @Query(
        """
        UPDATE artist_aliases
        SET target_key = :targetKey,
            target_artist_id = :targetArtistId,
            target_artist_name = :targetArtistName
        WHERE target_key = :sourceKey
        """,
    )
    abstract suspend fun moveGroup(
        sourceKey: String,
        targetKey: String,
        targetArtistId: String,
        targetArtistName: String,
    )

    @Query("DELETE FROM artist_aliases WHERE source_key = :sourceKey")
    abstract suspend fun deleteBySourceKey(sourceKey: String)

    @Query("DELETE FROM artist_aliases")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(aliases: List<ArtistAliasEntity>) {
        deleteAll()
        aliases.forEach { alias ->
            if (alias.sourceKey != alias.targetKey) upsert(alias)
        }
    }
}
