package com.catlytics.core.domain.repository

import com.catlytics.core.model.HomeRecommendationsSettings
import com.catlytics.core.model.RecentAddedWindow
import kotlinx.coroutines.flow.Flow

interface HomePreferencesRepository {
    fun observeHomeRecommendationsSettings(): Flow<HomeRecommendationsSettings>

    suspend fun setShowRecommendedPlaylists(show: Boolean)

    suspend fun setRecentAddedWindow(window: RecentAddedWindow)

    suspend fun setShowNewTrackBadge(show: Boolean)
}
