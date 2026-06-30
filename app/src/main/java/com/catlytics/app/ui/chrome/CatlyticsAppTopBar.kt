package com.catlytics.app.ui.chrome

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    searchPlaceholder: String = "Buscar",
    containerColor: Color? = null,
) {
    CatlyticsTopAppBar(
        containerColor = containerColor,
        title = {
            if (supportsSearch && isSearchExpanded) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.focusRequester(searchFocusRequester),
                    placeholder = { Text(searchPlaceholder) },
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
                            "Cerrar búsqueda"
                        } else {
                            searchPlaceholder
                        },
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Abrir ajustes",
                )
            }
        },
    )
}

@Composable
internal fun SettingsTopAppBar(
    title: String = "Ajustes",
    onBack: () -> Unit,
    containerColor: Color? = null,
) {
    CatlyticsTopAppBar(
        containerColor = containerColor,
        title = { TopAppBarTitle(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Volver",
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
    searchPlaceholder: String = "Buscar",
    searchFocusRequester: FocusRequester? = null,
) {
    CatlyticsTopAppBar(
        containerColor = containerColor,
        title = {
            if (supportsSearch && isSearchExpanded) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = if (searchFocusRequester != null) Modifier.focusRequester(searchFocusRequester) else Modifier,
                    placeholder = { Text(searchPlaceholder) },
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
                    contentDescription = "Volver",
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
                            "Cerrar búsqueda"
                        } else {
                            searchPlaceholder
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
