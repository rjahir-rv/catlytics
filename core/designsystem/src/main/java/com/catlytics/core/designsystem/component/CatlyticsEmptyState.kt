package com.catlytics.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R

@Composable
fun CatlyticsEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    messageColor: Color = MaterialTheme.colorScheme.onBackground,
    mascotSize: Dp = 200.dp,
    fillMaxSize: Boolean = true,
    action: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.then(if (fillMaxSize) Modifier.fillMaxSize() else Modifier.fillMaxWidth()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(catlyticsMascot()),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(mascotSize),
                )
                if (title != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = messageColor,
                    textAlign = TextAlign.Center,
                )
            }
            action?.invoke()
        }
    }
}

/**
 * Michi que contrasta con el fondo actual: el blanco sobre tema oscuro y el negro sobre claro.
 * Se decide por la luminancia del esquema para respetar el `ThemeMode` de la app y el color dinámico.
 */
@Composable
private fun catlyticsMascot(): Int =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        R.drawable.ic_michi_stereo
    } else {
        R.drawable.ic_michi_dj
    }
