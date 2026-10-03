package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.designsystem.text.asString
import com.catlytics.feature.home.impl.R as HomeR

@Composable
internal fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CatlyticsEmptyState(
        message = stringResource(HomeR.string.home_permission_required_message),
        modifier = modifier,
        action = {
            Button(onClick = onRequestPermission) {
                Text(text = stringResource(HomeR.string.home_permission_required_action))
            }
        },
    )
}

@Composable
internal fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun EmptyLibraryContent(modifier: Modifier = Modifier) {
    CatlyticsEmptyState(
        message = stringResource(HomeR.string.home_empty_library_message),
        modifier = modifier,
    )
}

@Composable
internal fun NoSearchResultsContent(modifier: Modifier = Modifier) {
    CatlyticsEmptyState(
        message = stringResource(HomeR.string.home_search_no_results_message),
        modifier = modifier,
        messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun ErrorContent(
    message: UiText,
    modifier: Modifier = Modifier,
) {
    CatlyticsEmptyState(
        message = message.asString(),
        modifier = modifier,
        messageColor = MaterialTheme.colorScheme.error,
    )
}
