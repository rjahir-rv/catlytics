package com.catlytics.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R

/** One entry of a [CatlyticsOptionsMenu]. */
sealed interface CatlyticsMenuEntry

data class CatlyticsMenuItem(
    val text: String,
    @param:DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val selected: Boolean? = null,
) : CatlyticsMenuEntry

data object CatlyticsMenuDivider : CatlyticsMenuEntry

/** The "⋮" button with its dropdown, styled like the track options menu. */
@Composable
fun CatlyticsOptionsMenu(
    entries: List<CatlyticsMenuEntry>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    CatlyticsOptionsMenu(
        entries = entries,
        contentDescription = contentDescription,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun CatlyticsOptionsMenu(
    entries: List<CatlyticsMenuEntry>,
    contentDescription: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(modifier = modifier) {
        IconButton(onClick = { onExpandedChange(true) }, enabled = enabled) {
            Icon(painter = painterResource(R.drawable.ic_options), contentDescription = contentDescription)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            entries.forEach { entry ->
                when (entry) {
                    CatlyticsMenuDivider -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    is CatlyticsMenuItem -> MenuItem(
                        item = entry,
                        onClick = {
                            onExpandedChange(false)
                            entry.onClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuItem(item: CatlyticsMenuItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        item.destructive -> colors.error
        item.selected == true -> colors.primary
        else -> colors.onSurface
    }
    DropdownMenuItem(
        text = { Text(text = item.text, color = textColor) },
        onClick = onClick,
        enabled = item.enabled,
        leadingIcon = {
            if (item.selected == null) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    tint = if (item.destructive) colors.error else colors.onSurfaceVariant,
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.selected) colors.primaryContainer else colors.surfaceContainer,
                    contentColor = if (item.selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        },
    )
}
