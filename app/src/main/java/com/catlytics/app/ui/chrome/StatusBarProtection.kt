package com.catlytics.app.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun StatusBarProtection(
    color: Color = MaterialTheme.colorScheme.background,
    modifier: Modifier = Modifier,
) {
    val height = with(LocalDensity.current) {
        (WindowInsets.statusBars.getTop(this) * 1.35f).toDp()
    }

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.96f),
                        color.copy(alpha = 0.72f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
