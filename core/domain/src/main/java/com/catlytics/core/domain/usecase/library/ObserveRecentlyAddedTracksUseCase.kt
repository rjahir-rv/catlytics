package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tracks added to the device within the last [RECENT_WINDOW_DAYS] days,
 * newest first.
 */
class ObserveRecentlyAddedTracksUseCase(
    private val libraryRepository: LibraryRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    operator fun invoke(): Flow<List<Track>> = libraryRepository.observeTracks().map { tracks ->
        val cutoffMillis = nowMillis() - RECENT_WINDOW_MILLIS
        tracks
            .filter { track ->
                val addedAtMillis = track.addedAtMillis ?: return@filter false
                addedAtMillis > cutoffMillis
            }
            .sortedByDescending { track -> track.addedAtMillis ?: 0L }
    }

    companion object {
        const val RECENT_WINDOW_DAYS = 21
        const val RECENT_WINDOW_MILLIS: Long = RECENT_WINDOW_DAYS * 24L * 60L * 60L * 1_000L
    }
}
