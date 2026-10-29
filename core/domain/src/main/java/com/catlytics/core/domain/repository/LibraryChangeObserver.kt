package com.catlytics.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Emits whenever the device music library changes, for example when new
 * tracks are downloaded or existing tracks are removed.
 */
interface LibraryChangeObserver {
    fun observeChanges(): Flow<Unit>
}
