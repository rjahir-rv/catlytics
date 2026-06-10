package com.catlytics.feature.library.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute : NavKey

@Serializable
data class LibraryFolderRoute(
    val folderId: String,
    val folderName: String,
) : NavKey
