package com.catlytics.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.catlytics.core.designsystem.theme.CatlyticsTheme

@OptIn(ExperimentalMaterial3Api::class)
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
        resolveScrolledContainerColor(resolvedContainerColor, scrolledContainerColor)

    if (scrollBehavior == null) {
        CatlyticsTopAppBarRow(
            title = title,
            modifier = modifier,
            containerColor = resolvedContainerColor,
            scrolledContainerColor = resolvedScrolledContainerColor,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = TopAppBarDefaults.windowInsets,
        )
    } else {
        val systemBarInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        val topInset = systemBarInsets
            .only(WindowInsetsSides.Top)
            .asPaddingValues()
            .calculateTopPadding()
        Box(
            modifier = modifier
                .drawBehind {
                    val collapsedFraction = scrollBehavior.state.collapsedFraction
                        .coerceIn(0f, 1f)
                    drawRect(
                        color = resolvedContainerColor,
                        size = Size(size.width, topInset.toPx()),
                        alpha = 1f - collapsedFraction,
                    )
                }
                .padding(top = topInset),
        ) {
            CatlyticsTopAppBarRow(
                title = title,
                modifier = Modifier.fillMaxWidth(),
                containerColor = resolvedContainerColor,
                scrolledContainerColor = resolvedScrolledContainerColor,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = systemBarInsets.only(WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatlyticsTopAppBarRow(
    title: @Composable () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    scrolledContainerColor: Color,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    windowInsets: WindowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = scrolledContainerColor,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = modifier,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
    )
}

/**
 * Resolves the color used by Material3 when content is scrolled under the top app bar.
 */
internal fun resolveScrolledContainerColor(
    containerColor: Color,
    scrolledContainerColor: Color?,
): Color = scrolledContainerColor ?: containerColor

@OptIn(ExperimentalMaterial3Api::class)
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
