package com.catlytics.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListDetailIndicesTest {
    private fun indices(vararg items: String) = findListDetailIndices(
        items = items.toList(),
        isList = { it.startsWith("list") },
        isDetail = { it.startsWith("detail") },
    )

    @Test
    fun listOnTopShowsPlaceholder() {
        assertEquals(ListDetailIndices(listIndex = 1, detailIndex = null), indices("home", "list"))
    }

    @Test
    fun detailOnTopPairsWithItsList() {
        assertEquals(ListDetailIndices(listIndex = 0, detailIndex = 1), indices("list", "detail"))
    }

    @Test
    fun nestedDetailsKeepNearestList() {
        assertEquals(
            ListDetailIndices(listIndex = 1, detailIndex = 3),
            indices("home", "list", "detail-artist", "detail-album"),
        )
    }

    @Test
    fun otherDestinationOnTopFallsBackToSinglePane() {
        assertNull(indices("list", "detail", "settings"))
    }

    @Test
    fun detailWithoutListInItsTabFallsBackToSinglePane() {
        assertNull(indices("list", "home", "detail"))
    }

    @Test
    fun emptyStackHasNoPanes() {
        assertNull(indices())
    }
}
