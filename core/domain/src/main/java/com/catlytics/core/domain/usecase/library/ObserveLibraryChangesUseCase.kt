package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryChangeObserver

class ObserveLibraryChangesUseCase(
    private val libraryChangeObserver: LibraryChangeObserver,
) {
    operator fun invoke() = libraryChangeObserver.observeChanges()
}
