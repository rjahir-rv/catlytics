package com.catlytics.feature.settings.impl

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.core.model.ThemeMode
import com.catlytics.feature.settings.impl.R as SettingsR

@Composable
internal fun Modifier.settingsEntrance(index: Int, animate: Boolean): Modifier =
    staggeredEntrance(index = index, animate = animate)

@Composable
internal fun SettingsAppHeader(
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        colorScheme.primaryContainer,
                        colorScheme.tertiaryContainer,
                        colorScheme.surfaceContainerLow,
                    ),
                ),
            )
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.cat_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colorScheme.surface.copy(alpha = 0.6f), CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(SettingsR.string.settings_profile_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(SettingsR.string.settings_profile_version, appVersion),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
internal fun ThemePreviewCard(
    themeMode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "themePreviewBorder",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
        label = "themePreviewLabel",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.95f,
        label = "themePreviewScale",
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(shape)
                .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = shape),
        ) {
            when (themeMode) {
                ThemeMode.Light -> ThemeMockup(LightMockup)
                ThemeMode.Dark -> ThemeMockup(DarkMockup)
                ThemeMode.System -> Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) { ThemeMockup(LightMockup) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) { ThemeMockup(DarkMockup) }
                }
            }
        }
        Text(
            text = stringResource(
                when (themeMode) {
                    ThemeMode.System -> SettingsR.string.settings_theme_system_short
                    ThemeMode.Light -> SettingsR.string.settings_theme_light
                    ThemeMode.Dark -> SettingsR.string.settings_theme_dark
                },
            ),
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            maxLines = 1,
        )
    }
}

private data class MockupColors(val background: Color, val surface: Color, val line: Color)

private val LightMockup = MockupColors(
    background = Color(0xFFF6F6F6),
    surface = Color(0xFFFFFFFF),
    line = Color(0xFFD6D6D6),
)
private val DarkMockup = MockupColors(
    background = Color(0xFF0E0E0E),
    surface = Color(0xFF1E1E1E),
    line = Color(0xFF3A3A3A),
)

/** Maqueta mínima de la app: cabecera, dos filas y un minirreproductor. */
@Composable
private fun ThemeMockup(colors: MockupColors) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(6.dp)
                .background(colors.line, CircleShape),
        )
        repeat(2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(colors.surface, RoundedCornerShape(3.dp)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(colors.line, CircleShape),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(5.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, RoundedCornerShape(2.dp)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(colors.line, CircleShape),
            )
        }
    }
}

