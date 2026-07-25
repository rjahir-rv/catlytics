package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.ListeningStreak
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveListeningStreakUseCase(
    private val playbackEventRepository: PlaybackEventRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    operator fun invoke(): Flow<ListeningStreak> {
        return playbackEventRepository.observeActiveListeningDays().map { days ->
            computeStreak(days, LocalDate.now(clock))
        }
    }

    companion object {
        /**
         * [activeDaysNewestFirst] must be distinct local dates sorted descending (newest first).
         *
         * Grace rule: if today has no activity, the streak may still start from yesterday
         * so it is not broken at midnight before the user listens again.
         */
        fun computeStreak(
            activeDaysNewestFirst: List<LocalDate>,
            today: LocalDate,
        ): ListeningStreak {
            if (activeDaysNewestFirst.isEmpty()) {
                return ListeningStreak(currentDays = 0, lastActiveDayEpochDay = null)
            }

            val activeSet = activeDaysNewestFirst.toSet()
            val lastActive = activeDaysNewestFirst.first()
            val lastEpochDay = lastActive.toEpochDay()

            val startDay = when {
                today in activeSet -> today
                today.minusDays(1) in activeSet -> today.minusDays(1)
                else -> {
                    return ListeningStreak(
                        currentDays = 0,
                        lastActiveDayEpochDay = lastEpochDay,
                    )
                }
            }

            var streak = 0
            var cursor = startDay
            while (cursor in activeSet) {
                streak++
                cursor = cursor.minusDays(1)
            }

            return ListeningStreak(
                currentDays = streak,
                lastActiveDayEpochDay = lastEpochDay,
            )
        }
    }
}
