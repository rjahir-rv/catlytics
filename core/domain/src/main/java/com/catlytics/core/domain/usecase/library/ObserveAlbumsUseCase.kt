package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository

class ObserveAlbumsUseCase(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke() = libraryRepository.observeAlbums()
}
