package com.catlytics.feature.home.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.designsystem.text.asString
import com.catlytics.feature.home.impl.R as HomeR

@Composable
internal fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyStateContent(
        message = stringResource(HomeR.string.home_permission_required_message),
        modifier = modifier,
        contentOffset = (-68).dp,
        action = {
            Button(onClick = onRequestPermission) {
                Text(text = stringResource(HomeR.string.home_permission_required_action))
            }
        },
    )
}

@Composable
internal fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun EmptyLibraryContent(modifier: Modifier = Modifier) {
    EmptyStateContent(
        message = stringResource(HomeR.string.home_empty_library_message),
        modifier = modifier,
        contentOffset = (-34).dp,
    )
}

@Composable
private fun EmptyStateContent(
    message: String,
    modifier: Modifier = Modifier,
    contentOffset: Dp = 0.dp,
    action: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.offset(y = contentOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.cat_background),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(200.dp),
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
            action?.invoke()
        }
    }
}

@Composable
internal fun NoSearchResultsContent(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(HomeR.string.home_search_no_results_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun ErrorContent(
    message: UiText,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = message.asString(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )
}
