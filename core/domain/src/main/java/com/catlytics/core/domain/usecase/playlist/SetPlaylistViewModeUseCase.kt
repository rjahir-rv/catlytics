package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.PlaylistViewMode
import javax.inject.Inject

class SetPlaylistViewModeUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(viewMode: PlaylistViewMode) {
        preferencesRepository.setPlaylistViewMode(viewMode)
    }
}
