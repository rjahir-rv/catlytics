package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.ArtistViewMode

class SetArtistViewModeUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(viewMode: ArtistViewMode) {
        preferencesRepository.setArtistViewMode(viewMode)
    }
}
