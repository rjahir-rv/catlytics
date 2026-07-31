package com.catlytics.app.playback

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.catlytics.app.ui.sheet.TrackOptionsDropdownMenu
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsTopAppBar
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
    onSeekBackward10Seconds: () -> Unit,
    onSeekForward10Seconds: () -> Unit,
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
                CatlyticsTopAppBar(
                    title = {
                        Text(
                            text = "Reproduciendo ahora",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    containerColor = Color.Transparent,
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                val useTwoColumns = maxWidth >= TWO_COLUMN_MIN_WIDTH
                val contentModifier = Modifier
                    .widthIn(max = NOW_PLAYING_MAX_WIDTH)
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (useTwoColumns) 32.dp else 20.dp,
                        vertical = 12.dp,
                    )

                if (useTwoColumns) {
                    Row(
                        modifier = contentModifier,
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NowPlayingArtwork(
                            track = track,
                            onArtworkLoaded = { artworkBitmap = it },
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 440.dp),
                        )
                        NowPlayingDetails(
                            playbackState = playbackState,
                            track = track,
                            isCurrentTrackLiked = isCurrentTrackLiked,
                            onAddCurrentTrackToLiked = onAddCurrentTrackToLiked,
                            onSeekTo = onSeekTo,
                            onSeekBackward10Seconds = onSeekBackward10Seconds,
                            onSeekForward10Seconds = onSeekForward10Seconds,
                            onToggleShuffle = onToggleShuffle,
                            onSkipPrevious = onSkipPrevious,
                            onTogglePlayback = onTogglePlayback,
                            onSkipNext = onSkipNext,
                            onCycleRepeatMode = onCycleRepeatMode,
                            onShareTrack = onShareTrack,
                            onOpenQueue = { isQueueVisible = true },
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 480.dp),
                        )
                    }
                } else {
                    Column(
                        modifier = contentModifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NowPlayingArtwork(
                            track = track,
                            onArtworkLoaded = { artworkBitmap = it },
                            modifier = Modifier
                                .widthIn(max = 420.dp)
                                .fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        NowPlayingDetails(
                            playbackState = playbackState,
                            track = track,
                            isCurrentTrackLiked = isCurrentTrackLiked,
                            onAddCurrentTrackToLiked = onAddCurrentTrackToLiked,
                            onSeekTo = onSeekTo,
                            onSeekBackward10Seconds = onSeekBackward10Seconds,
                            onSeekForward10Seconds = onSeekForward10Seconds,
                            onToggleShuffle = onToggleShuffle,
                            onSkipPrevious = onSkipPrevious,
                            onTogglePlayback = onTogglePlayback,
                            onSkipNext = onSkipNext,
                            onCycleRepeatMode = onCycleRepeatMode,
                            onShareTrack = onShareTrack,
                            onOpenQueue = { isQueueVisible = true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingDetails(
    playbackState: PlaybackState,
    track: Track?,
    isCurrentTrackLiked: Boolean,
    onAddCurrentTrackToLiked: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBackward10Seconds: () -> Unit,
    onSeekForward10Seconds: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onShareTrack: (Track) -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = track?.title ?: "Sin canción en reproducción",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track?.artist?.name ?: "Selecciona una canción para iniciar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconToggleButton(
                checked = isCurrentTrackLiked,
                onCheckedChange = { onAddCurrentTrackToLiked() },
                enabled = track != null,
                modifier = Modifier.size(48.dp),
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
                    modifier = Modifier.size(26.dp),
                    tint = if (isCurrentTrackLiked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        PlaybackProgress(
            positionMillis = playbackState.positionMillis,
            bufferedPositionMillis = playbackState.bufferedPositionMillis,
            durationMillis = playbackState.durationMillis,
            enabled = track != null,
            onSeekTo = onSeekTo,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlaybackControls(
            playbackState = playbackState,
            enabled = track != null,
            onSkipPrevious = onSkipPrevious,
            onSeekBackward10Seconds = onSeekBackward10Seconds,
            onTogglePlayback = onTogglePlayback,
            onSeekForward10Seconds = onSeekForward10Seconds,
            onSkipNext = onSkipNext,
        )

        Spacer(modifier = Modifier.height(24.dp))

        PlaybackSecondaryActions(
            playbackState = playbackState,
            track = track,
            hasQueue = playbackState.queue.isNotEmpty(),
            onToggleShuffle = onToggleShuffle,
            onShareTrack = onShareTrack,
            onOpenQueue = onOpenQueue,
            onCycleRepeatMode = onCycleRepeatMode,
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PlaybackControls(
    playbackState: PlaybackState,
    enabled: Boolean,
    onSkipPrevious: () -> Unit,
    onSeekBackward10Seconds: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekForward10Seconds: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPlayingOrBuffering =
        playbackState.status == PlaybackStatus.Playing ||
            playbackState.status == PlaybackStatus.Buffering

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onSkipPrevious,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_back),
                contentDescription = "Anterior",
                modifier = Modifier.size(30.dp),
            )
        }
        IconButton(
            onClick = onSeekBackward10Seconds,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_replay_10),
                contentDescription = "Retroceder 10 segundos",
                modifier = Modifier.size(28.dp),
            )
        }
        FilledIconButton(
            onClick = onTogglePlayback,
            enabled = enabled,
            modifier = Modifier.size(72.dp),
        ) {
            Crossfade(
                targetState = isPlayingOrBuffering,
                animationSpec = tween(durationMillis = PLAYBACK_ICON_CROSSFADE_MILLIS),
                label = "PlaybackIconCrossfade",
            ) { showPauseIcon ->
                Icon(
                    painter = if (showPauseIcon) {
                        painterResource(id = R.drawable.ic_pause)
                    } else {
                        painterResource(id = R.drawable.ic_play)
                    },
                    contentDescription = if (showPauseIcon) {
                        "Pausar"
                    } else {
                        "Reproducir"
                    },
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        IconButton(
            onClick = onSeekForward10Seconds,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_forward_10),
                contentDescription = "Adelantar 10 segundos",
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(
            onClick = onSkipNext,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_next),
                contentDescription = "Siguiente",
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun PlaybackSecondaryActions(
    playbackState: PlaybackState,
    track: Track?,
    hasQueue: Boolean,
    onToggleShuffle: () -> Unit,
    onShareTrack: (Track) -> Unit,
    onOpenQueue: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = playbackState.isShuffleEnabled,
            onCheckedChange = { onToggleShuffle() },
            enabled = track != null,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_shuffle),
                contentDescription = if (playbackState.isShuffleEnabled) {
                    "Desactivar mezcla"
                } else {
                    "Activar mezcla"
                },
                tint = if (playbackState.isShuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(
            onClick = { track?.let(onShareTrack) },
            enabled = track != null,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_share),
                contentDescription = "Compartir canción",
            )
        }
        IconButton(
            onClick = onOpenQueue,
            enabled = hasQueue,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_list),
                contentDescription = "Abrir cola de reproducción",
            )
        }
        IconButton(
            onClick = onCycleRepeatMode,
            enabled = track != null,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    stateDescription = when (playbackState.repeatMode) {
                        PlaybackRepeatMode.Off -> "Repetición desactivada"
                        PlaybackRepeatMode.One -> "Repitiendo canción"
                        PlaybackRepeatMode.All -> "Repitiendo cola"
                    }
                },
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
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
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
                .padding(4.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        AsyncImage(
            model = artworkRequest,
            contentDescription = track?.let { "Carátula de ${it.title}" },
            placeholder = painterResource(id = R.drawable.placeholder_track),
            error = painterResource(id = R.drawable.placeholder_track),
            fallback = painterResource(id = R.drawable.placeholder_track),
            onSuccess = { state ->
                onArtworkLoaded(state.result.image.toBitmap())
            },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = artworkShape,
                    clip = false,
                )
                .clip(artworkShape),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackProgress(
    positionMillis: Long,
    bufferedPositionMillis: Long,
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
    val bufferedProgress = bufferedPositionMillis.progressFor(durationMillis)

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
                            .size(12.dp)
                            .blur(
                                radius = 4.dp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded,
                            )
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                shape = CircleShape,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
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
                val trackEnabled = enabled && durationMillis > 0L
                val activeColor = if (trackEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                }
                val bufferedColor = if (trackEnabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                }
                val inactiveColor = if (trackEnabled) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(inactiveColor),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(
                                bufferedProgress
                                    .coerceAtLeast(sliderState.value)
                                    .coerceIn(0f, 1f),
                            )
                            .background(bufferedColor),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(sliderState.value.coerceIn(0f, 1f))
                            .background(activeColor),
                    )
                }
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

private val TWO_COLUMN_MIN_WIDTH = 640.dp
private val NOW_PLAYING_MAX_WIDTH = 1_040.dp
private const val PLAYBACK_ICON_CROSSFADE_MILLIS = 150
