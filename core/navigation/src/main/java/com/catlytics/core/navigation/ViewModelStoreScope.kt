package com.catlytics.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

@Composable
fun <T : Any> ViewModelStoreScope(
    key: T?,
    content: @Composable (T) -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    val entries = rememberDecoratedNavEntries(
        backStack = listOfNotNull(key),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = { entryKey -> NavEntry(entryKey) { currentContent(it) } },
    )
    entries.forEach { it.Content() }
}
