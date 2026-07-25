@file:OptIn(ExperimentalMaterial3Api::class)

package com.catlytics.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.catlytics.core.designsystem.theme.CatlyticsTheme

@Composable
fun CatlyticsTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    scrolledContainerColor: Color? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.background
    val resolvedScrolledContainerColor =
        scrolledContainerColor ?: resolvedContainerColor.copy(alpha = 0f)

    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = resolvedContainerColor,
            scrolledContainerColor = resolvedScrolledContainerColor,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = modifier,
        scrollBehavior = scrollBehavior,
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
