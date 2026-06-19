package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.SortDirection

class SetLibrarySortDirectionUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(direction: SortDirection) {
        preferencesRepository.setLibrarySortDirection(direction)
    }
}
