package com.catlytics.core.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.RecentlyPlayedTrack
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
               MAX(timestamp) AS lastListenedAtMillis
        FROM playback_events
        GROUP BY track_id
        ORDER BY lastListenedAtMillis DESC
        LIMIT :limit
    """)
    fun observeRecentlyPlayedTracks(limit: Int): Flow<List<RecentlyPlayedTrack>>

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

    @Query("""
        SELECT CASE CAST(strftime('%w', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER)
                   WHEN 0 THEN 7
                   ELSE CAST(strftime('%w', timestamp / 1000, 'unixepoch', 'localtime') AS INTEGER)
               END AS dayOfWeek,
               SUM(duration_listened_millis) AS totalListenedMillis
        FROM playback_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek
    """)
    fun observeDailyListening(startMillis: Long, endMillis: Long): Flow<List<DailyListeningStat>>

    @Query("""
        SELECT COUNT(*)
        FROM playback_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
    """)
    fun observePlayCount(startMillis: Long, endMillis: Long): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT track_id) AS trackCount,
               COUNT(DISTINCT artist_id) AS artistCount,
               COUNT(DISTINCT album_id) AS albumCount
        FROM playback_events
    """)
    fun observeListeningTotals(): Flow<ListeningTotals>

    @Query("DELETE FROM playback_events WHERE timestamp < :beforeMillis")
    suspend fun deleteEventsBefore(beforeMillis: Long): Int
}
