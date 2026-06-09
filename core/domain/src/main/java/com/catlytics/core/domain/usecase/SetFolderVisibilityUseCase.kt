package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.LibraryRepository

class SetFolderVisibilityUseCase(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(folderId: String, visible: Boolean) {
        libraryRepository.setFolderVisible(folderId, visible)
    }
}
