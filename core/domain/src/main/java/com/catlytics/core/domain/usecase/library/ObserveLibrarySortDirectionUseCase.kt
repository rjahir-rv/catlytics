package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.SortDirection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLibrarySortDirectionUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    operator fun invoke(): Flow<SortDirection> = preferencesRepository.observeLibrarySortDirection()
}
