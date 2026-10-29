package com.catlytics.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SearchCollapsibleHeader(
    isSearching: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = !isSearching,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        content()
    }
}

/** Hay búsqueda en curso si el campo está activo o ya tiene texto. */
fun isSearching(isSearchActive: Boolean, searchQuery: String): Boolean =
    isSearchActive || searchQuery.isNotBlank()
