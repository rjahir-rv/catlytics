package com.catlytics.app

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
    isHome: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchActionClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    CatlyticsTopAppBar(
        title = {
            if (isHome && isSearchExpanded) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.focusRequester(searchFocusRequester),
                    placeholder = { Text("Buscar canciones") },
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
            if (isHome) {
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
                            "Buscar canciones"
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
    onBack: () -> Unit,
) {
    CatlyticsTopAppBar(
        title = { TopAppBarTitle("Ajustes") },
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
private fun TopAppBarTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
    )
}
