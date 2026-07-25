package com.catlytics.core.data.repository

import com.catlytics.core.data.local.room.PlaybackEventDao
import com.catlytics.core.data.local.room.PlaybackEventEntity
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.PeriodUniqueCounts
import com.catlytics.core.model.RecentlyPlayedTrack
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.TopAlbum
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class RoomPlaybackEventRepository @Inject constructor(
    private val dao: PlaybackEventDao
) : PlaybackEventRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun recordEvent(event: PlaybackEvent) {
        withContext(ioDispatcher) {
            dao.insert(event.toEntity())
        }
    }

    override fun observeRecentlyPlayedTracks(limit: Int): Flow<List<RecentlyPlayedTrack>> {
        return dao.observeRecentlyPlayedTracks(limit).flowOn(ioDispatcher)
    }

    override fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>> {
        return dao.observeTopTracks(startMillis, endMillis, limit).flowOn(ioDispatcher)
    }

    override fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>> {
        return dao.observeTopArtists(startMillis, endMillis, limit).flowOn(ioDispatcher)
    }

    override fun observeTopAlbums(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopAlbum>> {
        return dao.observeTopAlbums(startMillis, endMillis, limit).flowOn(ioDispatcher)
    }

    override fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long> {
        return dao.observeTotalListeningTime(startMillis, endMillis).flowOn(ioDispatcher)
    }

    override fun observeDailyListening(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<DailyListeningStat>> {
        return dao.observeDailyListening(startMillis, endMillis).flowOn(ioDispatcher)
    }

    override fun observePlayCount(startMillis: Long, endMillis: Long): Flow<Int> {
        return dao.observePlayCount(startMillis, endMillis).flowOn(ioDispatcher)
    }

    override fun observePeriodUniqueCounts(
        startMillis: Long,
        endMillis: Long,
    ): Flow<PeriodUniqueCounts> {
        return dao.observePeriodUniqueCounts(startMillis, endMillis).flowOn(ioDispatcher)
    }

    override fun observeListeningTotals(): Flow<ListeningTotals> {
        return dao.observeListeningTotals().flowOn(ioDispatcher)
    }

    override fun observeActiveListeningDays(): Flow<List<LocalDate>> {
        return dao.observeActiveListeningDays()
            .map { days -> days.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun cleanOldEvents(beforeMillis: Long): Int {
        return withContext(ioDispatcher) {
            dao.deleteEventsBefore(beforeMillis)
        }
    }

    override suspend fun getAllEvents(): List<PlaybackEvent> {
        return withContext(ioDispatcher) {
            dao.getAllEvents().map { it.toDomain() }
        }
    }

    override suspend fun insertEvents(events: List<PlaybackEvent>) {
        if (events.isEmpty()) return
        withContext(ioDispatcher) {
            events.chunked(INSERT_BATCH_SIZE).forEach { batch ->
                dao.insertAll(batch.map { it.toEntity() })
            }
        }
    }

    override suspend fun deleteAllEvents() {
        withContext(ioDispatcher) {
            dao.deleteAllEvents()
        }
    }

    override fun observeBackupSummary(): Flow<StatisticsBackupSummary> {
        return dao.observeBackupSummary()
            .map { row ->
                StatisticsBackupSummary(
                    eventCount = row.eventCount,
                    firstEventMillis = row.firstEventMillis,
                    lastEventMillis = row.lastEventMillis,
                )
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getEventFingerprints(): Set<String> {
        return withContext(ioDispatcher) {
            dao.getEventFingerprints().mapTo(mutableSetOf()) { row ->
                playbackEventFingerprint(
                    trackId = row.trackId,
                    timestamp = row.timestamp,
                    durationListenedMillis = row.durationListenedMillis,
                )
            }
        }
    }

    companion object {
        private const val INSERT_BATCH_SIZE = 500
    }
}

internal fun playbackEventFingerprint(
    trackId: String,
    timestamp: Long,
    durationListenedMillis: Long,
): String = "$trackId|$timestamp|$durationListenedMillis"

private fun PlaybackEvent.toEntity(): PlaybackEventEntity {
    return PlaybackEventEntity(
        trackId = trackId,
        trackTitle = trackTitle,
        artistId = artistId,
        artistName = artistName,
        albumId = albumId,
        albumTitle = albumTitle,
        artworkUri = artworkUri,
        durationListenedMillis = durationListenedMillis,
        trackDurationMillis = trackDurationMillis,
        timestamp = timestamp
    )
}

private fun PlaybackEventEntity.toDomain(): PlaybackEvent {
    return PlaybackEvent(
        trackId = trackId,
        trackTitle = trackTitle,
        artistId = artistId,
        artistName = artistName,
        albumId = albumId,
        albumTitle = albumTitle,
        artworkUri = artworkUri,
        durationListenedMillis = durationListenedMillis,
        trackDurationMillis = trackDurationMillis,
        timestamp = timestamp,
    )
}
