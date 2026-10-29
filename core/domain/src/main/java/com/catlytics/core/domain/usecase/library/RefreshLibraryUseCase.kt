package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import javax.inject.Inject

class RefreshLibraryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(): Int = libraryRepository.refreshTracks()
}
