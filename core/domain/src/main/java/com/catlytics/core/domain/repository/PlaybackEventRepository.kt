package com.catlytics.core.domain.repository

import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import kotlinx.coroutines.flow.Flow

interface PlaybackEventRepository {
    suspend fun recordEvent(event: PlaybackEvent)
    fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>>
    fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>>
    fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long>
    fun observeDailyListening(startMillis: Long, endMillis: Long): Flow<List<DailyListeningStat>>
    fun observePlayCount(startMillis: Long, endMillis: Long): Flow<Int>
    fun observeListeningTotals(): Flow<ListeningTotals>
    suspend fun cleanOldEvents(beforeMillis: Long): Int
}
