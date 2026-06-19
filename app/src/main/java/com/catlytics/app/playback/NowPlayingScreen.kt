package com.catlytics.app.playback

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.catlytics.app.ui.sheet.TrackOptionsDropdownMenu
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.extractArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberFallbackArtworkGradientColors
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
    onRemoveQueueItem: (Int) -> Unit,
    onTrackOptions: (Track) -> Unit,
    canAddCurrentTrackToQueue: Boolean,
    onAddCurrentTrackToPlaylist: () -> Unit,
    onToggleCurrentTrackLikedFromOptions: () -> Unit,
    onAddCurrentTrackToQueue: () -> Unit,
    onGoToCurrentTrackAlbum: () -> Unit,
    onGoToCurrentTrackArtist: () -> Unit,
    isCurrentTrackLiked: Boolean,
    onAddCurrentTrackToLiked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playbackState.currentTrack
    val durationMillis = playbackState.durationMillis
    val positionMillis = playbackState.positionMillis
    val fallbackGradient = rememberFallbackArtworkGradientColors()
    var artworkBitmap by remember(track?.artworkUri) { mutableStateOf<Bitmap?>(null) }
    var gradientColors by remember { mutableStateOf(fallbackGradient) }
    var isQueueVisible by remember { mutableStateOf(false) }
    val onDismissQueue = remember { { isQueueVisible = false } }
    val animatedGradientColors = animateArtworkGradientColors(
        target = gradientColors,
        labelPrefix = "NowPlayingGradient",
    )

    LaunchedEffect(track?.artworkUri, artworkBitmap, fallbackGradient) {
        gradientColors = artworkBitmap?.extractArtworkGradientColors(fallbackGradient) ?: fallbackGradient
    }

    if (isQueueVisible) {
        PlaybackQueueBottomSheet(
            queue = playbackState.queue,
            currentTrackId = track?.id,
            gradientColors = animatedGradientColors,
            onDismiss = onDismissQueue,
            onPlayQueueItem = onPlayQueueItem,
            onMoveQueueItem = onMoveQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            onTrackOptions = onTrackOptions,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedGradientColors.start,
                        animatedGradientColors.center,
                        animatedGradientColors.end,
                    ),
                ),
            ),
    ) {
        Scaffold(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "REPRODUCIENDO",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_down),
                                contentDescription = "Volver",
                            )
                        }
                    },
                    actions = {
                        track?.let { currentTrack ->
                            TrackOptionsDropdownMenu(
                                track = currentTrack,
                                isLiked = isCurrentTrackLiked,
                                canAddToQueue = canAddCurrentTrackToQueue,
                                onAddToPlaylist = onAddCurrentTrackToPlaylist,
                                onToggleLiked = onToggleCurrentTrackLikedFromOptions,
                                onAddToQueue = onAddCurrentTrackToQueue,
                                onGoToAlbum = onGoToCurrentTrackAlbum,
                                onGoToArtist = onGoToCurrentTrackArtist,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NowPlayingArtwork(
                    track = track,
                    onArtworkLoaded = { artworkBitmap = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = track?.title ?: "Sin canción en reproducción",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track?.artist?.name ?: "selecciona una canción para iniciar",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onAddCurrentTrackToLiked,
                        enabled = track != null,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isCurrentTrackLiked) {
                                    R.drawable.ic_favorite_fill
                                } else {
                                    R.drawable.ic_favorite
                                },
                            ),
                            contentDescription = if (isCurrentTrackLiked) {
                                "Quitar de Tus me gusta"
                            } else {
                                "Agregar a Tus me gusta"
                            },
                            modifier = Modifier.size(28.dp),
                            tint = if (isCurrentTrackLiked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PlaybackProgress(
                    positionMillis = positionMillis,
                    durationMillis = durationMillis,
                    enabled = track != null,
                    onSeekTo = onSeekTo,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(48.dp),
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
                        modifier = Modifier.size(64.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_back),
                            contentDescription = "Anterior",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    FilledIconButton(
                        onClick = onTogglePlayback,
                        enabled = track != null,
                        modifier = Modifier.size(80.dp),
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
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    IconButton(
                        onClick = onSkipNext,
                        enabled = track != null,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_next),
                            contentDescription = "siguiente",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    IconButton(
                        onClick = onCycleRepeatMode,
                        modifier = Modifier.size(48.dp),
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

                Spacer(modifier = Modifier.height(32.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NowPlayingArtwork(
    track: Track?,
    onArtworkLoaded: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkShape = RoundedCornerShape(28.dp)
    val platformContext = LocalPlatformContext.current
    val artworkRequest = remember(platformContext, track?.artworkUri) {
        ImageRequest.Builder(platformContext)
            .data(track?.artworkUri)
            .allowHardware(false)
            .build()
    }

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .blur(
                    radius = 32.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                    shape = artworkShape,
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .blur(
                    radius = 16.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
                    shape = artworkShape,
                ),
        )
        AsyncImage(
            model = artworkRequest,
            contentDescription = track?.let { "Carátula de ${it.title}" },
            placeholder = painterResource(id = R.drawable.placeholder_album),
            error = painterResource(id = R.drawable.placeholder_album),
            fallback = painterResource(id = R.drawable.placeholder_album),
            onSuccess = { state ->
                onArtworkLoaded(state.result.image.toBitmap())
            },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(artworkShape),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val interactionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
        disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    )
    val displayedPositionMillis = displayedPositionMillis(
        isSeeking = isSeeking,
        pendingProgress = pendingProgress,
        positionMillis = positionMillis,
        durationMillis = durationMillis,
    )
    val positionText = displayedPositionMillis.formatDuration()
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
            colors = sliderColors,
            interactionSource = interactionSource,
            thumb = {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .blur(
                                radius = 8.dp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded,
                            )
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = CircleShape,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (enabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    colors = sliderColors,
                    enabled = enabled && durationMillis > 0L,
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = positionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = durationText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun displayedPositionMillis(
    isSeeking: Boolean,
    pendingProgress: Float,
    positionMillis: Long,
    durationMillis: Long,
): Long = if (isSeeking) {
    (durationMillis * pendingProgress).toLong().coerceIn(0L, durationMillis)
} else {
    positionMillis
}

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
