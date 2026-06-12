package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository

class ObserveArtistContentUseCase(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(artistId: String) = libraryRepository.observeArtistContent(artistId)
}
