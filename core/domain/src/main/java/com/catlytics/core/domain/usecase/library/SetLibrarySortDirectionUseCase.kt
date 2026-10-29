package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.SortDirection
import javax.inject.Inject

class SetLibrarySortDirectionUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(direction: SortDirection) {
        preferencesRepository.setLibrarySortDirection(direction)
    }
}
