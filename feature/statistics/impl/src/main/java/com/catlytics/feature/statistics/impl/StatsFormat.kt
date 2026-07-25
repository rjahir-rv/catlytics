package com.catlytics.feature.statistics.impl

internal fun formatListeningDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "$minutes min"
    }
}

internal fun formatPlayCountLabel(count: Int): String {
    return if (count == 1) "1 reproducción" else "$count reproducciones"
}
