package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository

class ObserveArtistViewModeUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    operator fun invoke() = preferencesRepository.observeArtistViewMode()
}
