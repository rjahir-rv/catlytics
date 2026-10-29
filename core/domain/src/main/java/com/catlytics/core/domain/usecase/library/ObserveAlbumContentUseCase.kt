package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveAlbumContentUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(albumId: String) = libraryRepository.observeAlbumContent(albumId)
}
