package com.catlytics.app.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.navigation.TopLevelDestination

@Composable
internal fun CatlyticsBottomBar(
    selectedRoute: Any,
    onDestinationSelected: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (colorScheme.background.luminance() < DARK_LUMINANCE_THRESHOLD) {
        Color.Black
    } else {
        Color.White
    }
    val selectedColor = colorScheme.primary
    val unselectedColor = colorScheme.onSurfaceVariant
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = selectedColor,
        selectedTextColor = selectedColor,
        unselectedIconColor = unselectedColor,
        unselectedTextColor = unselectedColor,
        indicatorColor = selectedColor.copy(alpha = SELECTED_INDICATOR_ALPHA),
    )

    Column(
        modifier = modifier.background(containerColor),
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            containerColor = containerColor,
            contentColor = unselectedColor,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        ) {
            TopLevelDestination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = selectedRoute == destination.route,
                    onClick = { onDestinationSelected(destination.route) },
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = destination.label,
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = itemColors,
                )
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars),
        )
    }
}

private const val DARK_LUMINANCE_THRESHOLD = 0.5f
private const val SELECTED_INDICATOR_ALPHA = 0.15f
