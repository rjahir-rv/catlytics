package com.catlytics.core.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackEventDao {

    @Insert
    suspend fun insert(event: PlaybackEventEntity)

    @Query("""
        SELECT track_id AS trackId, 
               track_title AS title, 
               artist_name AS artistName, 
               artwork_uri AS artworkUri, 
               COUNT(*) AS playCount, 
               SUM(duration_listened_millis) AS totalListenedMillis
        FROM playback_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        GROUP BY track_id
        ORDER BY playCount DESC, totalListenedMillis DESC
        LIMIT :limit
    """)
    fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>>

    @Query("""
        SELECT artist_id AS artistId, 
               artist_name AS name, 
               MAX(artwork_uri) AS artworkUri, 
               COUNT(*) AS playCount, 
               SUM(duration_listened_millis) AS totalListenedMillis
        FROM playback_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        GROUP BY artist_id
        ORDER BY totalListenedMillis DESC
        LIMIT :limit
    """)
    fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>>

    @Query("""
        SELECT COALESCE(SUM(duration_listened_millis), 0)
        FROM playback_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
    """)
    fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("DELETE FROM playback_events WHERE timestamp < :beforeMillis")
    suspend fun deleteEventsBefore(beforeMillis: Long): Int
}
