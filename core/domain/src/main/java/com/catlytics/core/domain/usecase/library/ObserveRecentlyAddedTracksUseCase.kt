package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Tracks added to the device within the configured [RecentAddedWindow][com.catlytics.core.model.RecentAddedWindow],
 * newest first. The window follows the user's home recommendations settings.
 */
class ObserveRecentlyAddedTracksUseCase(
    private val libraryRepository: LibraryRepository,
    private val homePreferencesRepository: HomePreferencesRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    operator fun invoke(): Flow<List<Track>> = combine(
        libraryRepository.observeTracks(),
        homePreferencesRepository.observeHomeRecommendationsSettings(),
    ) { tracks, settings ->
        val cutoffMillis = nowMillis() - settings.recentAddedWindow.windowMillis
        tracks
            .filter { track ->
                val addedAtMillis = track.addedAtMillis ?: return@filter false
                addedAtMillis > cutoffMillis
            }
            .sortedByDescending { track -> track.addedAtMillis ?: 0L }
    }
}
