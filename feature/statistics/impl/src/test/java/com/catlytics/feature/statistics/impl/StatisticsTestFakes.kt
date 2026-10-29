package com.catlytics.feature.statistics.impl

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val MINUTE = 60_000L
const val HOUR = 60 * MINUTE
const val DAY = 24 * HOUR

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

fun playbackEvent(
    timestamp: Long,
    trackId: String = "track-1",
    artistId: String = "artist-1",
    artistName: String = "Johann Sebastian Bach",
    albumId: String? = "album-1",
    listenedMillis: Long = 180_000L,
) = PlaybackEvent(
    trackId = trackId,
    trackTitle = "Title $trackId",
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumId?.let { "Album $it" },
    artworkUri = null,
    durationListenedMillis = listenedMillis,
    trackDurationMillis = listenedMillis,
    timestamp = timestamp,
)

class FakePlaybackEventRepository : PlaybackEventRepository {
    val events = MutableStateFlow(emptyList<PlaybackEvent>())
    var failure: Throwable? = null

    private fun inRange(startMillis: Long, endMillis: Long): Flow<List<PlaybackEvent>> =
        events.map { all ->
            failure?.let { throw it }
            all.filter { it.timestamp in startMillis until endMillis }
        }

    override suspend fun recordEvent(event: PlaybackEvent) {
        events.value += event
    }

    override fun observeRecentlyPlayedTracks(limit: Int): Flow<List<RecentlyPlayedTrack>> =
        events.map { emptyList() }

    override fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>> =
        inRange(startMillis, endMillis).map { list ->
            list.groupBy { it.trackId }
                .map { (id, plays) ->
                    TopTrack(
                        trackId = id,
                        title = plays.first().trackTitle,
                        artistName = plays.first().artistName,
                        artworkUri = null,
                        playCount = plays.size,
                        totalListenedMillis = plays.sumOf { it.durationListenedMillis },
                    )
                }
                .sortedByDescending { it.totalListenedMillis }
                .take(limit)
        }

    override fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>> =
        inRange(startMillis, endMillis).map { list ->
            list.groupBy { it.artistId }
                .map { (id, plays) ->
                    TopArtist(
                        artistId = id,
                        name = plays.first().artistName,
                        artworkUri = null,
                        playCount = plays.size,
                        totalListenedMillis = plays.sumOf { it.durationListenedMillis },
                    )
                }
                .sortedByDescending { it.totalListenedMillis }
                .take(limit)
        }

    override fun observeTopAlbums(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopAlbum>> =
        inRange(startMillis, endMillis).map { emptyList() }

    override fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long> =
        inRange(startMillis, endMillis).map { list -> list.sumOf { it.durationListenedMillis } }

    override fun observeDailyListening(startMillis: Long, endMillis: Long): Flow<List<DailyListeningStat>> =
        inRange(startMillis, endMillis).map { emptyList() }

    override fun observePlayCount(startMillis: Long, endMillis: Long): Flow<Int> =
        inRange(startMillis, endMillis).map { it.size }

    override fun observePeriodUniqueCounts(startMillis: Long, endMillis: Long): Flow<PeriodUniqueCounts> =
        inRange(startMillis, endMillis).map { it.uniqueCounts() }

    override fun observeListeningTotals(): Flow<ListeningTotals> =
        inRange(0L, Long.MAX_VALUE).map { list ->
            val counts = list.uniqueCounts()
            ListeningTotals(counts.trackCount, counts.artistCount, counts.albumCount)
        }

    override fun observeActiveListeningDays(): Flow<List<LocalDate>> =
        inRange(0L, Long.MAX_VALUE).map { list ->
            list.map { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
                .distinct()
                .sortedDescending()
        }

    override suspend fun getAllEvents(): List<PlaybackEvent> = events.value

    override suspend fun replaceEvents(events: List<PlaybackEvent>) {
        this.events.value = events
    }

    override suspend fun insertEventsIfAbsent(events: List<PlaybackEvent>): Int {
        val added = events - this.events.value.toSet()
        this.events.value += added
        return added.size
    }

    override fun observeBackupSummary(): Flow<StatisticsBackupSummary> =
        inRange(0L, Long.MAX_VALUE).map { list ->
            StatisticsBackupSummary(
                eventCount = list.size.toLong(),
                firstEventMillis = list.minOfOrNull { it.timestamp },
                lastEventMillis = list.maxOfOrNull { it.timestamp },
            )
        }

    private fun List<PlaybackEvent>.uniqueCounts() = PeriodUniqueCounts(
        trackCount = map { it.trackId }.distinct().size,
        artistCount = map { it.artistId }.distinct().size,
        albumCount = mapNotNull { it.albumId }.distinct().size,
    )
}
