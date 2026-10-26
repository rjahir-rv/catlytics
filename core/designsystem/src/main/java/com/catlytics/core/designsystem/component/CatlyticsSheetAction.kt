package com.catlytics.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role

@Composable
fun CatlyticsSheetAction(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val color = when {
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }.let { if (enabled) it else it.copy(alpha = DISABLED_ALPHA) }
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(painter = painterResource(icon), contentDescription = null) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = color,
            leadingIconColor = color,
        ),
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    )
}

private const val DISABLED_ALPHA = 0.38f
