package com.catlytics.app.playback

import com.catlytics.core.model.PlaybackState

/** The part of [PlaybackState] that changes on every progress tick. */
data class PlaybackProgress(
    val positionMillis: Long = 0L,
    val bufferedPositionMillis: Long = 0L,
    val durationMillis: Long = 0L,
) {
    val fraction: Float
        get() = if (durationMillis > 0L) {
            (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        } else {
            0f
        }
}

fun PlaybackState.toProgress() = PlaybackProgress(
    positionMillis = positionMillis,
    bufferedPositionMillis = bufferedPositionMillis,
    durationMillis = durationMillis,
)
