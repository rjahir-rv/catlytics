package com.catlytics.feature.statistics.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Duración legible con unidades traducibles (`Xh Ym`, `Xh`, `Y min`).
 *
 * Es `@Composable` porque las unidades (`h`, `m`, `min`) son copy y se resuelven con
 * recursos; todas las llamadas actuales ocurren dentro de composición.
 */
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
    pluralStringResource(R.plurals.stats_play_count, count, count)
