package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.LibraryRepository

class ObserveAlbumContentUseCase(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(albumId: String) = libraryRepository.observeAlbumContent(albumId)
}
