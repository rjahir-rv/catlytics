package com.catlytics.app.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
internal fun StatusBarProtection(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val protectionHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding() * 1.35f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(protectionHeight)
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
