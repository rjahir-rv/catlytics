package com.catlytics.feature.library.impl.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.LibraryFolder

@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!hasAudioPermission) {
        PermissionRequiredContent(
            onRequestPermission = onRequestPermission,
            modifier = modifier,
        )
        return
    }

    when (uiState) {
        LibraryUiState.Loading -> LoadingContent(modifier)
        LibraryUiState.Empty -> EmptyContent(modifier)
        is LibraryUiState.Error -> MessageContent(uiState.message, modifier)
        is LibraryUiState.Success -> LibraryFolderList(
            folders = uiState.folders,
            onFolderVisibilityChange = onFolderVisibilityChange,
            onFolderSelected = onFolderSelected,
            modifier = modifier,
        )
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Catlytics necesita permiso para encontrar tus carpetas musicales.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRequestPermission) {
            Text("Permitir acceso a música")
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    MessageContent(
        message = "No encontramos carpetas con música en este dispositivo.",
        modifier = modifier,
    )
}

@Composable
private fun MessageContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    CatlyticsTheme {
        LibraryScreen(
            uiState = LibraryUiState.Success(
                folders = listOf(
                    LibraryFolder(
                        id = "external:Music",
                        name = "Music",
                        path = "Music",
                        trackCount = 24,
                        isVisible = true,
                    ),
                    LibraryFolder(
                        id = "external:Android",
                        name = "Android",
                        path = "Android",
                        trackCount = 128,
                        isVisible = false,
                    ),
                ),
            ),
            hasAudioPermission = true,
            onRequestPermission = {},
            onFolderVisibilityChange = { _, _ -> },
            onFolderSelected = {},
        )
    }
}
