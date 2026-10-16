package com.catlytics.app.ui.chrome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.navigation.TopLevelDestination

@Composable
internal fun CatlyticsNavigationSuiteScaffold(
    selectedRoute: Any,
    onDestinationSelected: (NavKey) -> Unit,
    navigationSuiteType: NavigationSuiteType,
    modifier: Modifier = Modifier,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = remember(colorScheme.background) {
        navigationContainerColor(colorScheme.background)
    }
    val unselectedColor = colorScheme.onSurfaceVariant
    val itemColors = navigationItemColors(
        selectedColor = colorScheme.primary,
        unselectedColor = unselectedColor,
    )

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteType = navigationSuiteType,
        state = state,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            shortNavigationBarContainerColor = containerColor,
            shortNavigationBarContentColor = unselectedColor,
            wideNavigationRailColors = WideNavigationRailDefaults.colors(
                containerColor = containerColor,
                contentColor = unselectedColor,
            ),
        ),
        navigationItemVerticalArrangement = Arrangement.Center,
        navigationItems = {
            TopLevelDestination.entries.forEach { destination ->
                NavigationSuiteItem(
                    selected = selectedRoute == destination.route,
                    onClick = { onDestinationSelected(destination.route) },
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(destination.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    navigationSuiteType = navigationSuiteType,
                    colors = itemColors,
                )
            }
        },
        content = content,
    )
}

@Composable
private fun navigationItemColors(
    selectedColor: Color,
    unselectedColor: Color,
): NavigationItemColors = ShortNavigationBarItemDefaults.colors(
    selectedIconColor = selectedColor,
    selectedTextColor = selectedColor,
    selectedIndicatorColor = selectedColor.copy(alpha = SELECTED_INDICATOR_ALPHA),
    unselectedIconColor = unselectedColor,
    unselectedTextColor = unselectedColor,
)


private fun navigationContainerColor(background: Color): Color {
    val lightness = ((background.luminance() - DARK_BACKGROUND_LUMINANCE) /
        (LIGHT_BACKGROUND_LUMINANCE - DARK_BACKGROUND_LUMINANCE)).coerceIn(0f, 1f)
    return lerp(Color.Black, Color.White, lightness)
}

private const val DARK_BACKGROUND_LUMINANCE = 0.05f
private const val LIGHT_BACKGROUND_LUMINANCE = 0.85f
private const val SELECTED_INDICATOR_ALPHA = 0.15f
