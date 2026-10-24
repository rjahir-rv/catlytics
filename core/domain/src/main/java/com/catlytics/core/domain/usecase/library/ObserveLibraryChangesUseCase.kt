package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryChangeObserver
import javax.inject.Inject

class ObserveLibraryChangesUseCase @Inject constructor(
    private val libraryChangeObserver: LibraryChangeObserver,
) {
    operator fun invoke() = libraryChangeObserver.observeChanges()
}
