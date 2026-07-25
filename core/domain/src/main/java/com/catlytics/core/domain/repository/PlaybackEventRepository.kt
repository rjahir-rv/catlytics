package com.catlytics.core.domain.repository

import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.PeriodUniqueCounts
import com.catlytics.core.model.RecentlyPlayedTrack
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.TopAlbum
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PlaybackEventRepository {
    suspend fun recordEvent(event: PlaybackEvent)
    fun observeRecentlyPlayedTracks(limit: Int): Flow<List<RecentlyPlayedTrack>>
    fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>>
    fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>>
    fun observeTopAlbums(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopAlbum>>
    fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long>
    /**
     * Daily totals for [[startMillis], [endMillis]). [DailyListeningStat.dayIndex] is 1-based
     * from the local calendar day of [startMillis].
     */
    fun observeDailyListening(startMillis: Long, endMillis: Long): Flow<List<DailyListeningStat>>
    fun observePlayCount(startMillis: Long, endMillis: Long): Flow<Int>
    fun observePeriodUniqueCounts(startMillis: Long, endMillis: Long): Flow<PeriodUniqueCounts>
    fun observeListeningTotals(): Flow<ListeningTotals>

    /** Distinct local calendar days with activity, newest first. */
    fun observeActiveListeningDays(): Flow<List<LocalDate>>

    suspend fun cleanOldEvents(beforeMillis: Long): Int

    /** Full event stream for backup export. */
    suspend fun getAllEvents(): List<PlaybackEvent>

    /** Atomically replace the complete listening history. */
    suspend fun replaceEvents(events: List<PlaybackEvent>)

    /**
     * Atomically insert events whose natural fingerprint is not already stored.
     * Returns the number of newly inserted events.
     */
    suspend fun insertEventsIfAbsent(events: List<PlaybackEvent>): Int

    fun observeBackupSummary(): Flow<StatisticsBackupSummary>
}
