package com.catlytics.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val CATLYTICS_REPOSITORY_URL = "https://github.com/rjahir-rv/catlytics"

@Composable
internal fun AboutSettingsContent(
    appVersion: String,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = scaffoldContentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = bottomPadding() + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AboutCard(
                title = "Catlytics",
                description = "Versión $appVersion",
            )
        }
        item {
            AboutCard(
                title = "Música local y estadísticas",
                description = "Catlytics es un reproductor para la música almacenada en tu dispositivo. " +
                    "También registra estadísticas de escucha para consultar tu actividad de reproducción.",
            )
        }
        item {
            AboutCard(
                title = "Código abierto y sin anuncios",
                description = "La aplicación es un proyecto de código abierto y no incluye anuncios. " +
                    "El código fuente está disponible públicamente en GitHub.",
            ) {
                TextButton(onClick = { uriHandler.openUri(CATLYTICS_REPOSITORY_URL) }) {
                    Text("Ver repositorio en GitHub")
                }
            }
        }
    }
}

@Composable
private fun AboutCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
