package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository

class ObserveLibraryFoldersUseCase(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke() = libraryRepository.observeFolders()
}
