package com.catlytics.core.domain.repository

import com.catlytics.core.model.PlaybackSessionSnapshot
import kotlinx.coroutines.flow.Flow

interface PlaybackSessionRepository {
    fun observeSession(): Flow<PlaybackSessionSnapshot?>

    suspend fun saveSession(snapshot: PlaybackSessionSnapshot)

    suspend fun clearSession()
}
