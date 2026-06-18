package com.catlytics.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.catlytics.core.designsystem.theme.CatlyticsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatlyticsTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.background

    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = resolvedContainerColor,
            scrolledContainerColor = resolvedContainerColor,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun CatlyticsTopAppBarPreview() {
    CatlyticsTheme {
        CatlyticsTopAppBar(
            title = {
                Text(
                    text = "Inicio",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
        )
    }
}
