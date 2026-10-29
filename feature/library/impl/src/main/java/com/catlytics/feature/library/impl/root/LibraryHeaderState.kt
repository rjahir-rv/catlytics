package com.catlytics.feature.library.impl.root

import androidx.annotation.StringRes
import com.catlytics.core.model.LibraryFolder
import com.catlytics.feature.library.impl.R as LibraryR

internal enum class LibrarySection(@param:StringRes val labelRes: Int) {
    Albums(LibraryR.string.library_tab_albums),
    Artists(LibraryR.string.library_tab_artists),
    Folders(LibraryR.string.library_tab_folders),
}

internal data class FolderVisibilitySummary(
    val visible: Int,
    val hidden: Int,
)

internal fun List<LibraryFolder>.visibilitySummary(): FolderVisibilitySummary {
    val visible = count(LibraryFolder::isVisible)
    return FolderVisibilitySummary(visible = visible, hidden = size - visible)
}
