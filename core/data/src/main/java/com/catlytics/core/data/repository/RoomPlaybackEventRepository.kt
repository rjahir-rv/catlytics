package com.catlytics.core.data.repository

import com.catlytics.core.data.local.room.ArtistAliasDao
import com.catlytics.core.data.local.room.ArtistAliasEntity
import com.catlytics.core.data.local.room.ArtistArtworkOverrideDao
import com.catlytics.core.data.local.room.ArtistArtworkOverrideEntity
import com.catlytics.core.data.local.room.PlaybackEventDao
import com.catlytics.core.data.local.room.PlaybackEventEntity
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.PeriodUniqueCounts
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.RecentlyPlayedTrack
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.TopAlbum
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import com.catlytics.core.model.artistIdentityKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoomPlaybackEventRepository @Inject constructor(
    private val dao: PlaybackEventDao,
    private val artistArtworkDao: ArtistArtworkOverrideDao,
    private val artistAliasDao: ArtistAliasDao,
) : PlaybackEventRepository {
    override suspend fun recordEvent(event: PlaybackEvent) {
        dao.insert(event.toEntity())
    }

    override fun observeRecentlyPlayedTracks(limit: Int): Flow<List<RecentlyPlayedTrack>> {
        return dao.observeRecentlyPlayedTracks(limit)
    }

    override fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>> {
        return dao.observeTopTracks(startMillis, endMillis, limit)
    }

    override fun observeTrackPlayCounts(): Flow<Map<String, Int>> {
        return dao.observeTrackPlayCounts().map { rows -> rows.associate { it.trackId to it.playCount } }
    }

    override fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>> = combine(
        dao.observeTopArtists(startMillis, endMillis, limit),
        artistArtworkDao.observeAll(),
        artistAliasDao.observeAll(),
    ) { artists, artworkOverrides, aliases ->
        if (artworkOverrides.isEmpty()) return@combine artists
        val artwork = ArtistArtworkLookup(
            overrides = artworkOverrides.map(ArtistArtworkOverrideEntity::toDomain),
            aliases = aliases.map(ArtistAliasEntity::toDomain),
        )
        artists.map { artist ->
            artwork.artworkFor(artist.name)?.let { artist.copy(artworkUri = it.artworkUri) } ?: artist
        }
    }

    override fun observeTopAlbums(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopAlbum>> {
        return dao.observeTopAlbums(startMillis, endMillis, limit)
    }

    override fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long> {
        return dao.observeTotalListeningTime(startMillis, endMillis)
    }

    override fun observeDailyListening(
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<DailyListeningStat>> {
        return dao.observeDailyListening(startMillis, endMillis)
    }

    override fun observePlayCount(startMillis: Long, endMillis: Long): Flow<Int> {
        return dao.observePlayCount(startMillis, endMillis)
    }

    override fun observePeriodUniqueCounts(
        startMillis: Long,
        endMillis: Long,
    ): Flow<PeriodUniqueCounts> {
        return dao.observePeriodUniqueCounts(startMillis, endMillis)
    }

    override fun observeListeningTotals(): Flow<ListeningTotals> {
        return dao.observeListeningTotals()
    }

    override fun observeActiveListeningDays(): Flow<List<LocalDate>> {
        return dao.observeActiveListeningDays()
            .map { days -> days.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() } }
    }

    override suspend fun getAllEvents(): List<PlaybackEvent> {
        return dao.getAllEvents().map { it.toDomain() }
    }

    override suspend fun replaceEvents(events: List<PlaybackEvent>) {
        dao.replaceAllEvents(events.map { it.toEntity() })
    }

    override suspend fun insertEventsIfAbsent(events: List<PlaybackEvent>): Int =
        if (events.isEmpty()) 0 else dao.insertEventsIfAbsent(events.map { it.toEntity() })

    override fun observeBackupSummary(): Flow<StatisticsBackupSummary> {
        return dao.observeBackupSummary()
            .map { row ->
                StatisticsBackupSummary(
                    eventCount = row.eventCount,
                    firstEventMillis = row.firstEventMillis,
                    lastEventMillis = row.lastEventMillis,
                )
            }
    }
}

private fun PlaybackEvent.toEntity(): PlaybackEventEntity {
    return PlaybackEventEntity(
        trackId = trackId,
        trackTitle = trackTitle,
        artistId = artistId,
        artistName = artistName,
        artistKey = artistIdentityKey(artistName),
        albumId = albumId,
        albumTitle = albumTitle,
        artworkUri = artworkUri,
        durationListenedMillis = durationListenedMillis,
        trackDurationMillis = trackDurationMillis,
        timestamp = timestamp,
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
