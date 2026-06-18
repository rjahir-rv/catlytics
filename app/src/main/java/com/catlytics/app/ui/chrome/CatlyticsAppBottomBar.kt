package com.catlytics.app.ui.chrome

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.navigation.TopLevelDestination

@Composable
internal fun CatlyticsBottomBar(
    selectedRoute: Any,
    onDestinationSelected: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
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
                    Text(destination.label)
                },
            )
        }
    }
}
