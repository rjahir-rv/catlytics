package com.catlytics.feature.statistics.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.catlytics.core.designsystem.R as DesignSystemR

@Composable
internal fun formatListeningDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(
            R.string.stats_duration_hours_minutes,
            hours,
            minutes,
        )
        hours > 0 -> stringResource(R.string.stats_duration_hours, hours)
        else -> stringResource(R.string.stats_duration_minutes, minutes)
    }
}

@Composable
internal fun formatPlayCountLabel(count: Int): String =
    pluralStringResource(DesignSystemR.plurals.ds_play_count, count, count)
