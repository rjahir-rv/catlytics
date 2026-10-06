package com.catlytics.feature.library.impl.root

import com.catlytics.core.model.LibraryFolder
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryHeaderStateTest {

    @Test
    fun visibilitySummarySplitsVisibleAndHiddenFolders() {
        val folders = listOf(
            folder(id = "music", isVisible = true),
            folder(id = "podcasts", isVisible = false),
            folder(id = "downloads", isVisible = true),
        )

        assertEquals(
            FolderVisibilitySummary(visible = 2, hidden = 1),
            folders.visibilitySummary(),
        )
    }

    @Test
    fun visibilitySummaryOfEmptyListIsZero() {
        assertEquals(
            FolderVisibilitySummary(visible = 0, hidden = 0),
            emptyList<LibraryFolder>().visibilitySummary(),
        )
    }

    private fun folder(id: String, isVisible: Boolean) = LibraryFolder(
        id = id,
        name = id,
        path = id,
        trackCount = 1,
        isVisible = isVisible,
    )
}
