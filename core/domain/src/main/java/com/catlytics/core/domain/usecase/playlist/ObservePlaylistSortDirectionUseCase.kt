package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.SortDirection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaylistSortDirectionUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    operator fun invoke(): Flow<SortDirection> = preferencesRepository.observePlaylistSortDirection()
}
