package com.catlytics.core.data.repository

import com.catlytics.core.data.local.room.PlaybackEventDao
import com.catlytics.core.data.local.room.PlaybackEventEntity
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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

    override fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>> {
        return dao.observeTopTracks(startMillis, endMillis, limit).flowOn(ioDispatcher)
    }

    override fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>> {
        return dao.observeTopArtists(startMillis, endMillis, limit).flowOn(ioDispatcher)
    }

    override fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long> {
        return dao.observeTotalListeningTime(startMillis, endMillis).flowOn(ioDispatcher)
    }

    override suspend fun cleanOldEvents(beforeMillis: Long): Int {
        return withContext(ioDispatcher) {
            dao.deleteEventsBefore(beforeMillis)
        }
    }
}

private fun PlaybackEvent.toEntity(): PlaybackEventEntity {
    return PlaybackEventEntity(
        trackId = trackId,
        trackTitle = trackTitle,
        artistId = artistId,
        artistName = artistName,
        artworkUri = artworkUri,
        durationListenedMillis = durationListenedMillis,
        trackDurationMillis = trackDurationMillis,
        timestamp = timestamp
    )
}
