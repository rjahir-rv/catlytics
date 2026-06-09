package com.catlytics.feature.library.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.catlytics.core.designsystem.theme.CatlyticsTheme

@Composable
internal fun LibraryScreen(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize())
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    CatlyticsTheme {
        LibraryScreen()
    }
}
