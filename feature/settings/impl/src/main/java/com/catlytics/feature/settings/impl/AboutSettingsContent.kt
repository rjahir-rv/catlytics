package com.catlytics.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
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
        modifier = modifier.fillMaxSize(),
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
                title = stringResource(R.string.settings_about_app_title),
                description = stringResource(R.string.settings_about_version, appVersion),
            )
        }
        item {
            AboutCard(
                title = stringResource(R.string.settings_about_local_music_title),
                description = stringResource(R.string.settings_about_local_music_description),
            )
        }
        item {
            AboutCard(
                title = stringResource(R.string.settings_about_open_source_title),
                description = stringResource(R.string.settings_about_open_source_description),
            ) {
                TextButton(onClick = { uriHandler.openUri(CATLYTICS_REPOSITORY_URL) }) {
                    Text(stringResource(R.string.settings_about_repository_action))
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
