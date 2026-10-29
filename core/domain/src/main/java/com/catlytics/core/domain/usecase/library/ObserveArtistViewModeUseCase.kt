package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import javax.inject.Inject

class ObserveArtistViewModeUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    operator fun invoke() = preferencesRepository.observeArtistViewMode()
}
