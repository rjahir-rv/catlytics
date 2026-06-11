package com.catlytics.app.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onShareTrack: (Track) -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playbackState.currentTrack
    val durationMillis = playbackState.durationMillis
    val positionMillis = playbackState.positionMillis
    var isQueueVisible by remember { mutableStateOf(false) }

    if (isQueueVisible) {
        PlaybackQueueBottomSheet(
            queue = playbackState.queue,
            currentTrackId = track?.id,
            onDismiss = { isQueueVisible = false },
            onPlayQueueItem = onPlayQueueItem,
            onMoveQueueItem = onMoveQueueItem,
        )
    }

    Scaffold(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = { /* */ },
                colors = TopAppBarDefaults.topAppBarColors(Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_down),
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = track?.artworkUri,
                contentDescription = track?.let { "Carátula de ${it.title}" },
                placeholder = painterResource(id = R.drawable.placeholder_album),
                error = painterResource(id = R.drawable.placeholder_album),
                fallback = painterResource(id = R.drawable.placeholder_album),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.extraLarge),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = track?.title ?: "Sin canción en reproducción",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track?.artist?.name ?: "selecciona una canción para iniciar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(32.dp))

            PlaybackProgress(
                positionMillis = positionMillis,
                durationMillis = durationMillis,
                enabled = track != null,
                onSeekTo = onSeekTo,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(56.dp),
                    enabled = track != null,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shuffle_square),
                        contentDescription = if (playbackState.isShuffleEnabled) {
                            "Desactivar mezcla"
                        } else {
                            "Activar mezcla"
                        },
                        tint = if (playbackState.isShuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Unspecified
                        },
                    )
                }
                IconButton(
                    onClick = onSkipPrevious,
                    enabled = track != null,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_back),
                        contentDescription = "Anterior",
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlayback,
                    enabled = track != null,
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        painter = if (
                            playbackState.status == PlaybackStatus.Playing ||
                            playbackState.status == PlaybackStatus.Buffering
                        ) {
                            painterResource(id = R.drawable.ic_pause)
                        } else {
                            painterResource(id = R.drawable.ic_play)
                        },
                        contentDescription = if (playbackState.status == PlaybackStatus.Playing) {
                            "Pausar"
                        } else {
                            "Reproducir"
                        },
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    enabled = track != null,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = "siguiente",
                    )
                }
                IconButton(
                    onClick = onCycleRepeatMode,
                    modifier = Modifier.size(56.dp),
                    enabled = track != null,
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (playbackState.repeatMode == PlaybackRepeatMode.One) {
                                R.drawable.ic_repeat_one
                            } else {
                                R.drawable.ic_repeat_round
                            },
                        ),
                        contentDescription = when (playbackState.repeatMode) {
                            PlaybackRepeatMode.Off -> "Activar repetir canción"
                            PlaybackRepeatMode.One -> "Activar repetir todo"
                            PlaybackRepeatMode.All -> "Desactivar repetición"
                        },
                        tint = if (playbackState.repeatMode != PlaybackRepeatMode.Off) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { track?.let(onShareTrack) },
                    enabled = track != null,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = "Compartir canción",
                    )
                }
                IconButton(
                    onClick = { isQueueVisible = true },
                    enabled = playbackState.queue.isNotEmpty(),
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_list),
                        contentDescription = "Abrir cola de reproducción",
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackProgress(
    positionMillis: Long,
    durationMillis: Long,
    enabled: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingProgress by remember {
        mutableFloatStateOf(positionMillis.progressFor(durationMillis))
    }
    var isSeeking by remember { mutableStateOf(false) }
    val positionText = remember(positionMillis / MILLIS_PER_SECOND) {
        positionMillis.formatDuration()
    }
    val durationText = remember(durationMillis) {
        durationMillis.formatDuration()
    }

    LaunchedEffect(positionMillis, durationMillis) {
        if (!isSeeking) {
            pendingProgress = positionMillis.progressFor(durationMillis)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = pendingProgress,
            onValueChange = {
                isSeeking = true
                pendingProgress = it
            },
            onValueChangeFinished = {
                onSeekTo((durationMillis * pendingProgress).toLong())
                isSeeking = false
            },
            enabled = enabled && durationMillis > 0L,
            valueRange = 0f..1f,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = positionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val MILLIS_PER_SECOND = 1_000L

private fun Long.progressFor(durationMillis: Long): Float =
    if (durationMillis > 0L) {
        (toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        0f
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
