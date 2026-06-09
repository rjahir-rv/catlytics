package com.catlytics.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface LibraryPreferencesRepository {
    fun observeHiddenFolderIds(): Flow<Set<String>>

    suspend fun setFolderVisible(folderId: String, visible: Boolean)
}