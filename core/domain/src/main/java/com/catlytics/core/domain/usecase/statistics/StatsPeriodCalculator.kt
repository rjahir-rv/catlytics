package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.model.StatsGranularity
import com.catlytics.core.model.StatsPeriodRange
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object StatsPeriodCalculator {

    fun calculateRange(
        granularity: StatsGranularity,
        offset: Int,
        clock: Clock = Clock.systemDefaultZone(),
        locale: Locale = Locale.getDefault(),
    ): StatsPeriodRange {
        require(offset <= 0) { "Period offset cannot be in the future" }
        val zone = clock.zone
        val today = LocalDate.now(clock)

        return when (granularity) {
            StatsGranularity.WEEK -> {
                val startOfWeek = today
                    .with(DayOfWeek.MONDAY)
                    .plusWeeks(offset.toLong())
                val endOfWeek = startOfWeek.plusWeeks(1)
                val startMillis = startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = endOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
                val endInclusive = endOfWeek.minusDays(1)
                val label = buildWeekLabel(startOfWeek, endInclusive, locale)
                StatsPeriodRange(
                    granularity = granularity,
                    offset = offset,
                    startMillis = startMillis,
                    endMillis = endMillis,
                    label = label,
                )
            }

            StatsGranularity.MONTH -> {
                val yearMonth = YearMonth.from(today).plusMonths(offset.toLong())
                val startOfMonth = yearMonth.atDay(1)
                val endOfMonth = yearMonth.plusMonths(1).atDay(1)
                val startMillis = startOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = endOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
                val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, locale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                val label = "$monthName ${yearMonth.year}"
                StatsPeriodRange(
                    granularity = granularity,
                    offset = offset,
                    startMillis = startMillis,
                    endMillis = endMillis,
                    label = label,
                )
            }
        }
    }

    fun dayCount(range: StatsPeriodRange, clock: Clock = Clock.systemDefaultZone()): Int {
        return when (range.granularity) {
            StatsGranularity.WEEK -> 7
            StatsGranularity.MONTH -> {
                val zone = clock.zone
                val start = java.time.Instant.ofEpochMilli(range.startMillis).atZone(zone).toLocalDate()
                YearMonth.from(start).lengthOfMonth()
            }
        }
    }

    /**
     * Whether the user can navigate to an older period given the earliest event timestamp.
     * Returns false when there is no history or the previous period ends before the first event.
     */
    fun canNavigateBack(
        current: StatsPeriodRange,
        firstEventMillis: Long?,
        clock: Clock = Clock.systemDefaultZone(),
    ): Boolean {
        if (firstEventMillis == null) return false
        val previous = calculateRange(current.granularity, current.offset - 1, clock)
        // Allow going back if previous period still overlaps any history.
        return previous.endMillis > firstEventMillis
    }

    private fun buildWeekLabel(start: LocalDate, endInclusive: LocalDate, locale: Locale): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        return "${start.format(formatter)} – ${endInclusive.format(formatter)}"
    }
}
