package com.catlytics.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelBackStackTest {
    @Test
    fun replaceAboveSwapsNestedDetailsForNewDetail() {
        val backStack = TopLevelBackStack(ListKey)
        backStack.add(DetailKey("artist"))
        backStack.add(DetailKey("album"))

        backStack.replaceAbove(anchor = ListKey, key = DetailKey("other"))

        assertEquals(listOf(ListKey, DetailKey("other")), backStack.backStack.toList())
    }

    @Test
    fun replaceAboveWithoutAnchorPushesKey() {
        val backStack = TopLevelBackStack(ListKey)
        backStack.add(DetailKey("a"))

        backStack.replaceAbove(anchor = OtherTabKey, key = DetailKey("b"))

        assertEquals(listOf(ListKey, DetailKey("a"), DetailKey("b")), backStack.backStack.toList())
    }

    @Test
    fun removeLastOnTabRootFallsBackToPreviousTab() {
        val backStack = TopLevelBackStack(ListKey)
        backStack.add(DetailKey("a"))
        backStack.addTopLevel(OtherTabKey)

        backStack.removeLast()

        assertEquals(ListKey, backStack.topLevelKey)
        assertEquals(listOf(ListKey, DetailKey("a")), backStack.backStack.toList())
    }

    @Test
    fun serializerRestoresStacksAndCurrentTab() {
        val backStack = TopLevelBackStack(ListKey)
        backStack.add(DetailKey("a"))
        backStack.addTopLevel(OtherTabKey)
        backStack.add(DetailKey("b"))
        val serializer = TopLevelBackStackSerializer(startKey = ListKey)

        val restored = Json.decodeFromString(serializer, Json.encodeToString(serializer, backStack))

        assertEquals(OtherTabKey, restored.topLevelKey)
        assertEquals(backStack.backStack.toList(), restored.backStack.toList())

        restored.removeLast()
        restored.removeLast()
        assertEquals(ListKey, restored.topLevelKey)
    }

    @Test
    fun emptySnapshotFallsBackToStartKey() {
        val serializer = TopLevelBackStackSerializer(startKey = ListKey)

        val restored = Json.decodeFromString(serializer, "[]")

        assertEquals(listOf<NavKey>(ListKey), restored.backStack.toList())
    }
}

@Serializable
internal data object ListKey : NavKey

@Serializable
internal data object OtherTabKey : NavKey

@Serializable
internal data class DetailKey(val id: String) : NavKey
