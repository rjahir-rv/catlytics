package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.LibraryRepository

class ObserveFolderContentUseCase(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(folderId: String) = libraryRepository.observeFolderContent(folderId)
}
