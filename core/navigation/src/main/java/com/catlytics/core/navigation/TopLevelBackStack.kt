package com.catlytics.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

class TopLevelBackStack(
    startKey: NavKey,
) {
    private val topLevelStacks = linkedMapOf(
        startKey to mutableStateListOf(startKey),
    )

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(startKey)

    fun addTopLevel(key: NavKey) {
        val stack = topLevelStacks.remove(key) ?: mutableStateListOf(key)
        topLevelStacks[key] = stack
        topLevelKey = key
        updateBackStack()
    }

    fun add(key: NavKey) {
        val currentStack = topLevelStacks[topLevelKey] ?: mutableStateListOf(topLevelKey).also {
            topLevelStacks[topLevelKey] = it
        }
        currentStack.add(key)
        updateBackStack()
    }

    fun removeLast() {
        val currentStack = topLevelStacks[topLevelKey] ?: return

        if (currentStack.size <= 1) {
            if (topLevelStacks.size <= 1) return
            topLevelStacks.remove(topLevelKey)
            topLevelKey = topLevelStacks.keys.last()
            updateBackStack()
            return
        }

        currentStack.removeAt(currentStack.lastIndex)
        updateBackStack()
    }

    private fun updateBackStack() {
        backStack.clear()
        backStack.addAll(topLevelStacks.values.flatten())
    }
}
