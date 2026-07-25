package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.model.ListeningNarrative
import com.catlytics.core.model.PeriodStats

class BuildListeningNarrativeUseCase {

    operator fun invoke(stats: PeriodStats): ListeningNarrative {
        val total = stats.totalListenedMillis
        val topArtist = stats.topArtists.firstOrNull()
        val topTrack = stats.topTracks.firstOrNull()
        val eligible = total >= ELIGIBILITY_THRESHOLD_MILLIS &&
            (topArtist != null || topTrack != null)

        if (!eligible) {
            return ListeningNarrative(
                eligible = false,
                totalListenedMillis = total,
                topArtist = topArtist,
                topTrack = topTrack,
                headline = "",
                supportingLines = emptyList(),
            )
        }

        val headline = when {
            topArtist != null -> "Pasaste más tiempo con ${topArtist.name}"
            topTrack != null -> "Tu canción favorita fue ${topTrack.title}"
            else -> "Tu resumen de escucha"
        }

        val supportingLines = buildList {
            topTrack?.let { track ->
                add("Canción más escuchada · ${track.title} · ${formatPlayCount(track.playCount)}")
            }
            topArtist?.let { artist ->
                add("Artista más escuchado · ${artist.name} · ${formatDuration(artist.totalListenedMillis)}")
            }
        }

        return ListeningNarrative(
            eligible = true,
            totalListenedMillis = total,
            topArtist = topArtist,
            topTrack = topTrack,
            headline = headline,
            supportingLines = supportingLines,
        )
    }

    companion object {
        const val ELIGIBILITY_THRESHOLD_MILLIS: Long = 3_600_000L // 1 hour

        fun formatPlayCount(count: Int): String {
            return if (count == 1) "1 reproducción" else "$count reproducciones"
        }

        fun formatDuration(millis: Long): String {
            val totalMinutes = millis / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "$minutes min"
            }
        }
    }
}
