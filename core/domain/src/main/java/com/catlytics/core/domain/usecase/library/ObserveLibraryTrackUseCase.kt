package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveLibraryTrackUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(trackId: String): Flow<Track?> = libraryRepository.observeAllTracks()
        .map { tracks -> tracks.firstOrNull { it.id == trackId } }
        .distinctUntilChanged()
}
