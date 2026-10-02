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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.navigation.TopLevelDestination
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

@Composable
internal fun CatlyticsBottomBar(
    selectedRoute: Any,
    onDestinationSelected: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    glassEnabled: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    // Negro en tema oscuro y blanco en claro; interpolado por luminancia para que acompañe
    val containerColor = lerp(
        Color.Black,
        Color.White,
        ((colorScheme.background.luminance() - DARK_BACKGROUND_LUMINANCE) /
            (LIGHT_BACKGROUND_LUMINANCE - DARK_BACKGROUND_LUMINANCE)).coerceIn(0f, 1f),
    )
    val glass by animateFloatAsState(
        targetValue = if (hazeState != null && glassEnabled) 1f else 0f,
        animationSpec = tween(GLASS_TRANSITION_MILLIS),
        label = "bottomBarGlass",
    )
    val glassTint = colorScheme.background
    val lightness = ((colorScheme.background.luminance() - DARK_BACKGROUND_LUMINANCE) /
        (LIGHT_BACKGROUND_LUMINANCE - DARK_BACKGROUND_LUMINANCE)).coerceIn(0f, 1f)
    val glassTintAlpha = lerpFloat(GLASS_TINT_ALPHA_DARK, GLASS_TINT_ALPHA_LIGHT, lightness)
    val glassBlurRadius = lerpFloat(GLASS_BLUR_DARK, GLASS_BLUR_LIGHT, lightness).dp
    val glassModifier = if (hazeState != null && glass > 0f) {
        Modifier.hazeBlur(
            input = HazeInput.Sources(hazeState),
            style = HazeBlurStyle {
                blurRadius(glassBlurRadius)
                noiseFactor(0.04f)
                colorEffects(listOf(HazeColorEffect.tint(glassTint.copy(alpha = glassTintAlpha))))
                fallbackColorEffect(HazeColorEffect.tint(glassTint.copy(alpha = GLASS_FALLBACK_ALPHA)))
            },
        )
    } else {
        Modifier
    }
    val solidColor = containerColor.copy(alpha = 1f - glass)
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
        modifier = modifier
            .then(glassModifier)
            .background(solidColor),
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            containerColor = Color.Transparent,
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
                            contentDescription = stringResource(destination.labelRes),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(destination.labelRes),
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

private const val DARK_BACKGROUND_LUMINANCE = 0.05f
private const val LIGHT_BACKGROUND_LUMINANCE = 0.85f
private const val SELECTED_INDICATOR_ALPHA = 0.15f
private const val GLASS_TRANSITION_MILLIS = 300
private const val GLASS_TINT_ALPHA_LIGHT = 0.35f
private const val GLASS_TINT_ALPHA_DARK = 0.1f
private const val GLASS_BLUR_LIGHT = 24f
private const val GLASS_BLUR_DARK = 12f
private const val GLASS_FALLBACK_ALPHA = 0.95f
