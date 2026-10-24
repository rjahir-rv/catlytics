package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import javax.inject.Inject

class SetFolderVisibilityUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(folderId: String, visible: Boolean) {
        libraryRepository.setFolderVisible(folderId, visible)
    }
}
