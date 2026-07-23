package com.catlytics.core.domain.usecase.home

import com.catlytics.core.model.Track
import java.time.LocalDate
import kotlin.random.Random

class GenerateDailyPlaylistUseCase(
    private val currentDate: () -> LocalDate = LocalDate::now,
) {
    operator fun invoke(tracks: List<Track>): List<Track> {
        val distinctTracks = tracks.distinctBy(Track::id)
        if (distinctTracks.size < MIN_TRACK_COUNT) return emptyList()

        val sortedTracks = distinctTracks.sortedBy(Track::id)
        val seed = sortedTracks.fold(currentDate().toEpochDay()) { value, track ->
            value * 31L + track.id.hashCode()
        }
        return sortedTracks.shuffled(Random(seed.toInt())).take(MAX_TRACK_COUNT)
    }

    companion object {
        const val MIN_TRACK_COUNT = 5
        const val MAX_TRACK_COUNT = 10
    }
}
