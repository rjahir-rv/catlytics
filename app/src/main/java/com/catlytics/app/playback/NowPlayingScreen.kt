package com.catlytics.app.playback

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.catlytics.app.R as AppR
import com.catlytics.app.ui.sheet.TrackOptionsDropdownMenu
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsTopAppBar
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.extractArtworkAccentColor
import com.catlytics.core.designsystem.component.extractArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberFallbackArtworkGradientColors
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    modifier: Modifier = Modifier,
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
    onPlayNextCurrentTrack: () -> Unit = {},
    onAddCurrentTrackToQueue: () -> Unit,
    onGoToCurrentTrackAlbum: () -> Unit,
    onGoToCurrentTrackArtist: () -> Unit,
    isCurrentTrackLiked: Boolean,
    onAddCurrentTrackToLiked: () -> Unit,
    queueSourceTitle: String? = null,
    sleepTimerState: SleepTimerState = SleepTimerState.Inactive,
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
) {
    val track = playbackState.currentTrack
    val fallbackGradient = rememberFallbackArtworkGradientColors()
    val primary = MaterialTheme.colorScheme.primary
    var artworkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var gradientColors by remember { mutableStateOf(fallbackGradient) }
    var accentTarget by remember { mutableStateOf(Color.Unspecified) }
    var isQueueVisible by remember { mutableStateOf(false) }
    val onDismissQueue = remember { { isQueueVisible = false } }
    val animatedGradientColors = animateArtworkGradientColors(
        target = gradientColors,
        labelPrefix = "NowPlayingGradient",
    )
    // Mismo acento que el mini player: color vibrante de la carátula, o primary sin carátula.
    val accent by animateColorAsState(
        targetValue = if (accentTarget.isSpecified) accentTarget else primary,
        animationSpec = tween(ACCENT_ANIMATION_MILLIS),
        label = "NowPlayingAccent",
    )
    val isPlayingOrBuffering =
        playbackState.status == PlaybackStatus.Playing ||
            playbackState.status == PlaybackStatus.Buffering
    val upNext = remember(playbackState.queue, playbackState.currentIndex, playbackState.repeatMode) {
        upNextTrack(playbackState.queue, playbackState.currentIndex, playbackState.repeatMode)
    }

    LaunchedEffect(track?.id, track?.artworkUri) {
        if (track?.artworkUri == null) {
            artworkBitmap = null
        }
    }

    LaunchedEffect(artworkBitmap, fallbackGradient) {
        val bitmap = artworkBitmap
        gradientColors = bitmap?.extractArtworkGradientColors(fallbackGradient) ?: fallbackGradient
        accentTarget = bitmap?.extractArtworkAccentColor() ?: Color.Unspecified
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
            accent = accent,
            isPlaying = isPlayingOrBuffering,
            isShuffleEnabled = playbackState.isShuffleEnabled,
            repeatMode = playbackState.repeatMode,
            positionMillis = playbackState.positionMillis,
            sleepTimerState = sleepTimerState,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeatMode = onCycleRepeatMode,
            onStartSleepTimer = onStartSleepTimer,
            onCancelSleepTimer = onCancelSleepTimer,
        )
    }

    val dismissState = rememberSwipeToDismissState(onDismiss = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(dismissState.connection)
            .graphicsLayer {
                translationY = dismissState.offset
                alpha = 1f - (dismissState.offset / size.height).coerceIn(0f, 1f) * 0.5f
            }
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
                    title = { NowPlayingTitle(queueSourceTitle = queueSourceTitle) },
                    containerColor = Color.Transparent,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_down),
                                contentDescription = stringResource(AppR.string.app_action_back),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { track?.let(onShareTrack) },
                            enabled = track != null,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = stringResource(AppR.string.app_action_share_track),
                            )
                        }
                        track?.let { currentTrack ->
                            TrackOptionsDropdownMenu(
                                track = currentTrack,
                                isLiked = isCurrentTrackLiked,
                                canAddToQueue = canAddCurrentTrackToQueue,
                                onAddToPlaylist = onAddCurrentTrackToPlaylist,
                                onToggleLiked = onToggleCurrentTrackLikedFromOptions,
                                onPlayNext = onPlayNextCurrentTrack,
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
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                val useTwoColumns = maxWidth >= TWO_COLUMN_MIN_WIDTH
                val horizontalPadding = if (useTwoColumns) 32.dp else 24.dp
                val contentWidth = minOf(maxWidth, NOW_PLAYING_MAX_WIDTH) - horizontalPadding * 2
                val contentHeight = maxHeight - CONTENT_VERTICAL_PADDING * 2
                // La carátula se ajusta al alto disponible para que los controles quepan sin scroll;
                // el scroll queda solo como respaldo en pantallas muy bajas.
                val artworkSize = if (useTwoColumns) {
                    minOf((contentWidth - TWO_COLUMN_SPACING) / 2, contentHeight, 440.dp)
                } else {
                    minOf(contentWidth, contentHeight - DETAILS_MIN_HEIGHT, 420.dp)
                }.coerceAtLeast(MIN_ARTWORK_SIZE)
                val contentModifier = Modifier
                    .widthIn(max = NOW_PLAYING_MAX_WIDTH)
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .padding(horizontal = horizontalPadding, vertical = CONTENT_VERTICAL_PADDING)

                val artwork = @Composable { artworkModifier: Modifier ->
                    NowPlayingArtwork(
                        track = track,
                        isPlaying = isPlayingOrBuffering,
                        accent = accent,
                        onArtworkLoaded = { artworkBitmap = it },
                        onSkipPrevious = onSkipPrevious,
                        onSkipNext = onSkipNext,
                        onSeekBackward10Seconds = onSeekBackward10Seconds,
                        onSeekForward10Seconds = onSeekForward10Seconds,
                        modifier = artworkModifier
                            .size(artworkSize)
                            .staggeredEntrance(index = 0),
                    )
                }
                val details = @Composable { detailsModifier: Modifier ->
                    NowPlayingDetails(
                        playbackState = playbackState,
                        track = track,
                        upNext = upNext,
                        isSleepTimerActive = sleepTimerState is SleepTimerState.Active,
                        accent = accent,
                        isCurrentTrackLiked = isCurrentTrackLiked,
                        onAddCurrentTrackToLiked = onAddCurrentTrackToLiked,
                        onGoToCurrentTrackArtist = onGoToCurrentTrackArtist,
                        onGoToCurrentTrackAlbum = onGoToCurrentTrackAlbum,
                        onSeekTo = onSeekTo,
                        onSeekBackward10Seconds = onSeekBackward10Seconds,
                        onSeekForward10Seconds = onSeekForward10Seconds,
                        onToggleShuffle = onToggleShuffle,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayback = onTogglePlayback,
                        onSkipNext = onSkipNext,
                        onCycleRepeatMode = onCycleRepeatMode,
                        onOpenQueue = { isQueueVisible = true },
                        modifier = detailsModifier,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (useTwoColumns) {
                        Row(
                            modifier = contentModifier,
                            horizontalArrangement = Arrangement.spacedBy(
                                TWO_COLUMN_SPACING,
                                Alignment.CenterHorizontally,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            artwork(Modifier)
                            details(
                                Modifier
                                    .weight(1f)
                                    .widthIn(max = 480.dp),
                            )
                        }
                    } else {
                        Column(
                            modifier = contentModifier,
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            artwork(Modifier)
                            Spacer(modifier = Modifier.height(16.dp))
                            details(Modifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingTitle(queueSourceTitle: String?) {
    if (queueSourceTitle == null) {
        Text(
            text = stringResource(AppR.string.app_now_playing_title),
            style = MaterialTheme.typography.titleMedium,
        )
        return
    }
    Column {
        Text(
            text = stringResource(AppR.string.app_now_playing_from),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = queueSourceTitle,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NowPlayingDetails(
    playbackState: PlaybackState,
    track: Track?,
    upNext: Track?,
    isSleepTimerActive: Boolean,
    accent: Color,
    isCurrentTrackLiked: Boolean,
    onAddCurrentTrackToLiked: () -> Unit,
    onGoToCurrentTrackArtist: () -> Unit,
    onGoToCurrentTrackAlbum: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBackward10Seconds: () -> Unit,
    onSeekForward10Seconds: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NowPlayingTrackInfo(
            track = track,
            isLiked = isCurrentTrackLiked,
            onToggleLiked = onAddCurrentTrackToLiked,
            onGoToArtist = onGoToCurrentTrackArtist,
            onGoToAlbum = onGoToCurrentTrackAlbum,
            modifier = Modifier.staggeredEntrance(index = 1),
        )

        Spacer(modifier = Modifier.height(16.dp))

        NowPlayingProgress(
            positionMillis = playbackState.positionMillis,
            bufferedPositionMillis = playbackState.bufferedPositionMillis,
            durationMillis = playbackState.durationMillis,
            enabled = track != null,
            accent = accent,
            onSeekTo = onSeekTo,
            modifier = Modifier.staggeredEntrance(index = 2),
        )

        Spacer(modifier = Modifier.height(8.dp))

        NowPlayingTransportControls(
            playbackState = playbackState,
            enabled = track != null,
            accent = accent,
            onToggleShuffle = onToggleShuffle,
            onSkipPrevious = onSkipPrevious,
            onTogglePlayback = onTogglePlayback,
            onSkipNext = onSkipNext,
            onCycleRepeatMode = onCycleRepeatMode,
            modifier = Modifier.staggeredEntrance(index = 3),
        )

        Spacer(modifier = Modifier.height(12.dp))

        NowPlayingActionBar(
            track = track,
            upNext = upNext,
            isSleepTimerActive = isSleepTimerActive,
            hasQueue = playbackState.queue.isNotEmpty(),
            onSeekBackward10Seconds = onSeekBackward10Seconds,
            onSeekForward10Seconds = onSeekForward10Seconds,
            onOpenQueue = onOpenQueue,
            modifier = Modifier.staggeredEntrance(index = 4),
        )
    }
}

@Composable
private fun NowPlayingTrackInfo(
    track: Track?,
    isLiked: Boolean,
    onToggleLiked: () -> Unit,
    onGoToArtist: () -> Unit,
    onGoToAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkShape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = track?.title ?: stringResource(AppR.string.app_now_playing_empty_track),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = MARQUEE_DELAY_MILLIS,
                    repeatDelayMillis = MARQUEE_DELAY_MILLIS,
                ),
            )
            Text(
                text = track?.artist?.name
                    ?: stringResource(AppR.string.app_now_playing_empty_artist),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(linkShape)
                    .clickable(
                        enabled = track != null,
                        role = Role.Button,
                        onClickLabel = stringResource(AppR.string.app_track_option_go_to_artist),
                        onClick = onGoToArtist,
                    ),
            )
            track?.albumTitle?.takeIf { it.isNotBlank() }?.let { albumTitle ->
                Text(
                    text = albumTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(linkShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(AppR.string.app_track_option_go_to_album),
                            onClick = onGoToAlbum,
                        ),
                )
            }
        }
        LikeButton(
            isLiked = isLiked,
            enabled = track != null,
            onToggle = onToggleLiked,
        )
    }
}

@Composable
private fun LikeButton(
    isLiked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    var hasUserToggled by remember { mutableStateOf(false) }

    // Rebote solo cuando el usuario marca "me gusta", no al componer ni al cambiar de pista.
    LaunchedEffect(isLiked) {
        if (isLiked && hasUserToggled) {
            scale.animateTo(LIKE_BOUNCE_SCALE, tween(durationMillis = 120))
            scale.animateTo(1f, spring(dampingRatio = 0.4f))
        }
    }

    IconToggleButton(
        checked = isLiked,
        onCheckedChange = {
            hasUserToggled = true
            haptics.performHapticFeedback(
                if (isLiked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
            )
            onToggle()
        },
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        Icon(
            painter = painterResource(
                id = if (isLiked) R.drawable.ic_favorite_fill else R.drawable.ic_favorite,
            ),
            contentDescription = if (isLiked) {
                stringResource(AppR.string.app_action_remove_from_liked)
            } else {
                stringResource(AppR.string.app_action_add_to_liked)
            },
            modifier = Modifier.size(26.dp),
            tint = if (isLiked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * Deslizar hacia abajo para cerrar: consume el scroll sobrante hacia abajo (cuando el contenido
 * ya está arriba del todo) y cierra al soltar si se superó el umbral o la velocidad.
 */
private class SwipeToDismissState(
    private val thresholdPx: Float,
    private val velocityThreshold: Float,
    private val onDismiss: () -> Unit,
) {
    var offset by mutableFloatStateOf(0f)
        private set
    private var dismissed = false

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < 0f && offset > 0f) {
                val consumed = maxOf(available.y, -offset)
                offset += consumed
                return Offset(0f, consumed)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (!dismissed && source == NestedScrollSource.UserInput && available.y > 0f) {
                offset += available.y
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offset <= 0f || dismissed) return Velocity.Zero
            if (offset >= thresholdPx || available.y >= velocityThreshold) {
                dismissed = true
                onDismiss()
            } else {
                animate(initialValue = offset, targetValue = 0f, animationSpec = spring()) { value, _ ->
                    offset = value
                }
            }
            return available
        }
    }
}

@Composable
private fun rememberSwipeToDismissState(onDismiss: () -> Unit): SwipeToDismissState {
    val density = LocalDensity.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    return remember(density) {
        SwipeToDismissState(
            thresholdPx = with(density) { DISMISS_DISTANCE.toPx() },
            velocityThreshold = with(density) { DISMISS_VELOCITY.toPx() },
            onDismiss = { currentOnDismiss() },
        )
    }
}

private val TWO_COLUMN_MIN_WIDTH = 640.dp
private val NOW_PLAYING_MAX_WIDTH = 1_040.dp
private val TWO_COLUMN_SPACING = 40.dp
private val CONTENT_VERTICAL_PADDING = 12.dp
private val MIN_ARTWORK_SIZE = 160.dp

/** Alto aproximado de info + progreso + controles + acciones, usado para dimensionar la carátula. */
private val DETAILS_MIN_HEIGHT = 340.dp
private val DISMISS_DISTANCE = 120.dp
private val DISMISS_VELOCITY = 1_200.dp
private const val ACCENT_ANIMATION_MILLIS = 600
private const val MARQUEE_DELAY_MILLIS = 1_500
private const val LIKE_BOUNCE_SCALE = 1.25f
