package com.catlytics.feature.home.impl

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permission = rememberRequiredAudioPermission()
    var hasAudioPermission by remember(permission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasAudioPermission = isGranted
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.refreshLibrary()
        }
    }

    HomeScreen(
        uiState = uiState,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = { permissionLauncher.launch(permission) },
        onTrackSelected = viewModel::onTrackSelected,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onTrackSelected: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Inicio",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (!hasAudioPermission) {
            PermissionRequiredContent(onRequestPermission = onRequestPermission)
            return@Column
        }

        when (uiState) {
            HomeUiState.Empty -> EmptyLibraryContent()
            is HomeUiState.Error -> ErrorContent(message = uiState.message)
            HomeUiState.Loading -> LoadingContent()
            is HomeUiState.Success -> TrackList(
                tracks = uiState.tracks,
                onTrackSelected = onTrackSelected,
            )
        }
    }
}

@Composable
private fun rememberRequiredAudioPermission(): String = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Catlytics necesita permiso para leer tu musica local.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(onClick = onRequestPermission) {
            Text(text = "Permitir acceso a musica")
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyLibraryContent(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = "No encontramos canciones en este dispositivo.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    onTrackSelected: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            items = tracks,
            key = { it.id },
        ) { track ->
            TrackRow(
                track = track,
                onTrackSelected = { onTrackSelected(track, tracks) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    onTrackSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTrackSelected)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = track.artist.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = track.durationMillis.formatDuration(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onTrackSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play),
                contentDescription = "Reproducir ${track.title}",
            )
        }
    }
}

private fun Long.formatDuration(): String {
    val duration = milliseconds
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    CatlyticsTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                tracks = listOf(
                    Track(
                        id = "track-preview",
                        title = "Cancion local",
                        artist = Artist(
                            id = "artist-preview",
                            name = "Artista local",
                        ),
                        durationMillis = 186_000,
                        mediaUri = "content://media/external/audio/media/1",
                    ),
                ),
            ),
            hasAudioPermission = true,
            onRequestPermission = {},
            onTrackSelected = { _, _ -> },
        )
    }
}
