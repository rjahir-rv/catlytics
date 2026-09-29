package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.model.ListeningNarrative
import com.catlytics.core.model.ListeningNarrativeKind
import com.catlytics.core.model.PeriodStats

class BuildListeningNarrativeUseCase {

    operator fun invoke(stats: PeriodStats): ListeningNarrative {
        val total = stats.totalListenedMillis
        val topArtist = stats.topArtists.firstOrNull()
        val topTrack = stats.topTracks.firstOrNull()
        val eligible = total >= ELIGIBILITY_THRESHOLD_MILLIS &&
            (topArtist != null || topTrack != null)

        val kind = when {
            topArtist != null -> ListeningNarrativeKind.TimeWithArtist
            topTrack != null -> ListeningNarrativeKind.FavoriteTrack
            else -> ListeningNarrativeKind.Summary
        }

        return ListeningNarrative(
            eligible = eligible,
            totalListenedMillis = total,
            topArtist = topArtist,
            topTrack = topTrack,
            kind = kind,
        )
    }

    companion object {
        const val ELIGIBILITY_THRESHOLD_MILLIS: Long = 3_600_000L // 1 hour
    }
}
