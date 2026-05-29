package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.LibraryRepository

class RefreshLibraryUseCase(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke() {
        libraryRepository.refreshTracks()
    }
}
