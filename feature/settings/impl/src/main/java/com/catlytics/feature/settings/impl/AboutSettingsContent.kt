package com.catlytics.feature.settings.impl

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import com.catlytics.feature.settings.impl.components.SettingsSection
import com.catlytics.core.designsystem.R as DesignR

private const val CATLYTICS_REPOSITORY_URL = "https://github.com/rjahir-rv/catlytics"

@Composable
internal fun AboutSettingsContent(
    appVersion: String,
    onMetadataEditsClick: () -> Unit,
    onLicensesClick: () -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(CatlyticsLayoutTokens.CardMinColumnWidth),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = scaffoldContentPadding.calculateTopPadding() + 24.dp,
            end = 20.dp,
            bottom = bottomPadding() + 80.dp,
        ),
        verticalItemSpacing = 16.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            AboutHero(appVersion = appVersion)
        }
        item {
            AboutSection(
                title = stringResource(R.string.settings_about_local_music_title),
                iconRes = DesignR.drawable.ic_library,
                description = stringResource(R.string.settings_about_local_music_description),
            )
        }
        item {
            AboutSection(
                title = stringResource(R.string.settings_about_statistics_title),
                iconRes = DesignR.drawable.ic_line_chart,
                description = stringResource(R.string.settings_about_statistics_description),
            )
        }
        item {
            AboutSection(
                title = stringResource(R.string.settings_about_editing_title),
                iconRes = DesignR.drawable.ic_edit,
                description = stringResource(R.string.settings_about_editing_description),
                actionLabel = stringResource(R.string.settings_about_editing_action),
                onAction = onMetadataEditsClick,
            ) {
                AboutFeature(
                    iconRes = DesignR.drawable.ic_queue_music,
                    title = stringResource(R.string.settings_about_editing_tracks_title),
                    description = stringResource(R.string.settings_about_editing_tracks_description),
                )
                AboutFeature(
                    iconRes = DesignR.drawable.ic_album,
                    title = stringResource(R.string.settings_about_editing_albums_title),
                    description = stringResource(R.string.settings_about_editing_albums_description),
                )
                AboutFeature(
                    iconRes = DesignR.drawable.ic_artist,
                    title = stringResource(R.string.settings_about_editing_artists_title),
                    description = stringResource(R.string.settings_about_editing_artists_description),
                )
                Text(
                    text = stringResource(R.string.settings_about_editing_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AboutSection(
                title = stringResource(R.string.settings_about_open_source_title),
                iconRes = DesignR.drawable.ic_favorite,
                description = stringResource(R.string.settings_about_open_source_description),
                actionLabel = stringResource(R.string.settings_about_repository_action),
                onAction = { uriHandler.openUri(CATLYTICS_REPOSITORY_URL) },
            )
        }
        item {
            AboutSection(
                title = stringResource(R.string.settings_licenses_title),
                iconRes = DesignR.drawable.ic_info,
                description = stringResource(R.string.settings_licenses_description),
                actionLabel = stringResource(R.string.settings_licenses_action),
                onAction = onLicensesClick,
            )
        }
    }
}

@Composable
private fun AboutHero(
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
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
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            bitmap = rememberAppIcon(size = 88.dp),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape),
        )
        Text(
            text = stringResource(R.string.settings_about_app_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(R.string.settings_about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Surface(
            shape = CircleShape,
            color = colorScheme.surface.copy(alpha = 0.6f),
        ) {
            Text(
                text = stringResource(R.string.settings_about_version, appVersion),
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    @DrawableRes iconRes: Int,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    SettingsSection(title = title, iconRes = iconRes, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
            if (actionLabel != null) {
                FilledTonalButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun AboutFeature(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
