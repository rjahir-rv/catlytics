package com.catlytics.core.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.PeriodUniqueCounts
import com.catlytics.core.model.RecentlyPlayedTrack
import com.catlytics.core.model.TopAlbum
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import kotlinx.coroutines.flow.Flow

data class PlaybackEventFingerprintRow(
    val trackId: String,
    val timestamp: Long,
    val durationListenedMillis: Long,
)

data class PlaybackEventBoundsRow(
    val eventCount: Long,
    val firstEventMillis: Long?,
    val lastEventMillis: Long?,
)

@Dao
interface PlaybackEventDao {

    @Insert
    suspend fun insert(event: PlaybackEventEntity)

    @Insert
    suspend fun insertAll(events: List<PlaybackEventEntity>)

    @Transaction
    suspend fun replaceAllEvents(events: List<PlaybackEventEntity>) {
        deleteAllEvents()
        events.chunked(IMPORT_BATCH_SIZE).forEach { insertAll(it) }
    }

    @Transaction
    suspend fun insertEventsIfAbsent(events: List<PlaybackEventEntity>): Int {
        val fingerprints = getEventFingerprints().toMutableSet()
        val newEvents = events.filter { event ->
            fingerprints.add(
                PlaybackEventFingerprintRow(
                    trackId = event.trackId,
                    timestamp = event.timestamp,
                    durationListenedMillis = event.durationListenedMillis,
                ),
            )
        }
        newEvents.chunked(IMPORT_BATCH_SIZE).forEach { insertAll(it) }
        return newEvents.size
    }

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
        SELECT album_id AS albumId,
               album_title AS title,
               artist_name AS artistName,
               MAX(artwork_uri) AS artworkUri,
               COUNT(*) AS playCount,
               SUM(duration_listened_millis) AS totalListenedMillis
        FROM playback_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
          AND album_id IS NOT NULL
        GROUP BY album_id
        ORDER BY totalListenedMillis DESC, playCount DESC
        LIMIT :limit
    """)
    fun observeTopAlbums(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopAlbum>>

    @Query("""
        SELECT COALESCE(SUM(duration_listened_millis), 0)
        FROM playback_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
    """)
    fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long>

    /**
     * Daily totals within [[startMillis], [endMillis]), with [DailyListeningStat.dayIndex]
     * 1-based from the local calendar day of [startMillis] (1 = first day of the period).
     * Works for both week and month ranges.
     */
    @Query("""
        SELECT CAST(
                   ROUND(
                       julianday(date(timestamp / 1000, 'unixepoch', 'localtime'))
                       - julianday(date(:startMillis / 1000, 'unixepoch', 'localtime'))
                   )
                   AS INTEGER
               ) + 1 AS dayIndex,
               SUM(duration_listened_millis) AS totalListenedMillis
        FROM playback_events
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
        GROUP BY 1
        ORDER BY 1
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
        WHERE timestamp >= :startMillis AND timestamp < :endMillis
    """)
    fun observePeriodUniqueCounts(
        startMillis: Long,
        endMillis: Long,
    ): Flow<PeriodUniqueCounts>

    @Query("""
        SELECT COUNT(DISTINCT track_id) AS trackCount,
               COUNT(DISTINCT artist_id) AS artistCount,
               COUNT(DISTINCT album_id) AS albumCount
        FROM playback_events
    """)
    fun observeListeningTotals(): Flow<ListeningTotals>

    /**
     * Distinct local calendar days with at least one event, newest first (yyyy-MM-dd).
     */
    @Query("""
        SELECT DISTINCT date(timestamp / 1000, 'unixepoch', 'localtime') AS day
        FROM playback_events
        ORDER BY day DESC
    """)
    fun observeActiveListeningDays(): Flow<List<String>>

    @Query("DELETE FROM playback_events WHERE timestamp < :beforeMillis")
    suspend fun deleteEventsBefore(beforeMillis: Long): Int

    @Query("SELECT * FROM playback_events ORDER BY timestamp ASC")
    suspend fun getAllEvents(): List<PlaybackEventEntity>

    @Query("DELETE FROM playback_events")
    suspend fun deleteAllEvents()

    @Query(
        """
        SELECT COUNT(*) AS eventCount,
               MIN(timestamp) AS firstEventMillis,
               MAX(timestamp) AS lastEventMillis
        FROM playback_events
        """,
    )
    fun observeBackupSummary(): Flow<PlaybackEventBoundsRow>

    @Query(
        """
        SELECT track_id AS trackId,
               timestamp AS timestamp,
               duration_listened_millis AS durationListenedMillis
        FROM playback_events
        """,
    )
    suspend fun getEventFingerprints(): List<PlaybackEventFingerprintRow>

    private companion object {
        const val IMPORT_BATCH_SIZE = 500
    }
}
