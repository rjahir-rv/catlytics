@file:OptIn(ExperimentalMaterial3Api::class)

package com.catlytics.app.ui.chrome

import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsTopAppBar

@Composable
internal fun TopLevelTopAppBar(
    title: String,
    supportsSearch: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchActionClick: () -> Unit,
    onSettingsClick: () -> Unit,
    @StringRes searchPlaceholderRes: Int = AppR.string.app_search_placeholder_default,
    containerColor: Color? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CatlyticsTopAppBar(
        containerColor = containerColor,
        scrollBehavior = scrollBehavior,
        title = {
            if (supportsSearch && isSearchExpanded) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.focusRequester(searchFocusRequester),
                    placeholder = { Text(stringResource(searchPlaceholderRes)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            } else {
                TopAppBarTitle(title)
            }
        },
        actions = {
            if (supportsSearch) {
                IconButton(onClick = onSearchActionClick) {
                    Icon(
                        painter = painterResource(
                            if (isSearchExpanded) {
                                R.drawable.ic_close
                            } else {
                                R.drawable.ic_search
                            },
                        ),
                        contentDescription = if (isSearchExpanded) {
                            stringResource(AppR.string.app_action_close_search)
                        } else {
                            stringResource(searchPlaceholderRes)
                        },
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(AppR.string.app_action_open_settings),
                )
            }
        },
    )
}

@Composable
internal fun SettingsTopAppBar(
    title: String,
    onBack: () -> Unit,
    containerColor: Color? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CatlyticsTopAppBar(
        containerColor = containerColor,
        scrollBehavior = scrollBehavior,
        title = { TopAppBarTitle(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(AppR.string.app_action_back),
                )
            }
        },
    )
}

@Composable
internal fun LibraryDetailTopAppBar(
    title: String,
    onBack: () -> Unit,
    containerColor: Color? = null,
    supportsSearch: Boolean = false,
    isSearchExpanded: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchActionClick: () -> Unit = {},
    @StringRes searchPlaceholderRes: Int = AppR.string.app_search_placeholder_default,
    searchFocusRequester: FocusRequester? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.background

    CatlyticsTopAppBar(
        containerColor = resolvedContainerColor,
        scrolledContainerColor = resolvedContainerColor,
        scrollBehavior = scrollBehavior,
        title = {
            if (supportsSearch && isSearchExpanded) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = if (searchFocusRequester != null) Modifier.focusRequester(searchFocusRequester) else Modifier,
                    placeholder = { Text(stringResource(searchPlaceholderRes)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            } else if (title.isNotBlank()) {
                TopAppBarTitle(title)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(AppR.string.app_action_back),
                )
            }
        },
        actions = {
            if (supportsSearch) {
                IconButton(onClick = onSearchActionClick) {
                    Icon(
                        painter = painterResource(
                            if (isSearchExpanded) {
                                R.drawable.ic_close
                            } else {
                                R.drawable.ic_search
                            },
                        ),
                        contentDescription = if (isSearchExpanded) {
                            stringResource(AppR.string.app_action_close_search)
                        } else {
                            stringResource(searchPlaceholderRes)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun TopAppBarTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
    )
}
