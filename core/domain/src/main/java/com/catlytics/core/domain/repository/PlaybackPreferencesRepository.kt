package com.catlytics.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface PlaybackPreferencesRepository {
    fun observeCrossfadeDurationSeconds(): Flow<Int>

    suspend fun setCrossfadeDurationSeconds(seconds: Int)

    companion object {
        const val DEFAULT_CROSSFADE_DURATION_SECONDS = 0
        const val MIN_CROSSFADE_DURATION_SECONDS = 0
        const val MAX_CROSSFADE_DURATION_SECONDS = 12
    }
}
