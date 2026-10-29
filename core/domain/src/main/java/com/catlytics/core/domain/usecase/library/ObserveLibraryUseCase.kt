package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveLibraryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke() = libraryRepository.observeTracks()
}
