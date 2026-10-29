package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveFolderContentUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(folderId: String) = libraryRepository.observeFolderContent(folderId)
}
