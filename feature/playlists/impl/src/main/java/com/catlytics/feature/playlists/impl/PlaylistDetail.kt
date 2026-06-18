package com.catlytics.feature.playlists.impl

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.extractArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberFallbackArtworkGradientColors
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.playlist.RemoveTrackFromPlaylistUseCase
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.PlaylistContent
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data object NotFound : PlaylistDetailUiState
    data class Success(val content: PlaylistContent) : PlaylistDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class PlaylistDetailViewModel @Inject constructor(
    private val observeContent: ObservePlaylistContentUseCase,
    private val removeTrack: RemoveTrackFromPlaylistUseCase,
    private val playTrack: PlayTrackUseCase,
    observePlaybackState: ObservePlaybackStateUseCase,
    private val togglePlaybackUseCase: TogglePlaybackUseCase,
) : ViewModel() {
    private val playlistId = MutableStateFlow<String?>(null)
    val uiState = playlistId
        .filterNotNull()
        .flatMapLatest(observeContent::invoke)
        .map { content ->
            content?.let(PlaylistDetailUiState::Success) ?: PlaylistDetailUiState.NotFound
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PlaylistDetailUiState.Loading,
        )

    val playbackState = observePlaybackState().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlaybackState(),
    )

    fun open(id: String) {
        playlistId.value = id
    }

    fun remove(trackId: String) =
        viewModelScope.launch { playlistId.value?.let { removeTrack(it, trackId) } }

    fun play(track: Track, queue: List<Track>) = viewModelScope.launch {
        val source = playlistId.value
            ?.let(PlaybackQueueSource::Playlist)
            ?: PlaybackQueueSource.Static
        playTrack(track, queue, source)
    }

    fun togglePlayback() = viewModelScope.launch { togglePlaybackUseCase() }
}

@Composable
internal fun PlaylistDetailRoute(
    playlistId: String,
    onTrackOptions: (track: Track, onRemoveFromPlaylist: () -> Unit) -> Unit,
    onTopBarColorChange: (Color) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(key = playlistId),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    LaunchedEffect(playlistId) { viewModel.open(playlistId) }
    PlaylistDetailScreen(
        uiState = uiState,
        playbackState = playbackState,
        onPlay = viewModel::play,
        onTrackOptions = { track ->
            onTrackOptions(track) {
                viewModel.remove(track.id)
                Toast.makeText(
                    context,
                    "${track.title} eliminada de la playlist",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        onTogglePlayback = viewModel::togglePlayback,
        onTopBarColorChange = onTopBarColorChange,
    )
}

@Composable
private fun PlaylistDetailScreen(
    uiState: PlaylistDetailUiState,
    playbackState: PlaybackState,
    onPlay: (Track, List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onTogglePlayback: () -> Unit,
    onTopBarColorChange: (Color) -> Unit,
) {
    when (uiState) {
        PlaylistDetailUiState.Loading -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return
        }

        PlaylistDetailUiState.NotFound -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("Playlist no disponible.") }
            return
        }

        is PlaylistDetailUiState.Success -> Unit
    }

    val content = uiState.content
    val platformContext = LocalPlatformContext.current
    val fallbackGradient = rememberFallbackArtworkGradientColors()
    val artworkRequest = remember(platformContext, content.playlist.artworkUri) {
        ImageRequest.Builder(platformContext)
            .data(content.playlist.artworkUri)
            .allowHardware(false)
            .build()
    }
    var artworkBitmap by remember(content.playlist.artworkUri) { mutableStateOf<Bitmap?>(null) }
    var gradientColors by remember { mutableStateOf(fallbackGradient) }
    val animatedGradientColors = animateArtworkGradientColors(
        target = gradientColors,
        labelPrefix = "PlaylistDetailGradient",
    )

    LaunchedEffect(animatedGradientColors.start) {
        onTopBarColorChange(animatedGradientColors.start)
    }

    LaunchedEffect(content.playlist.artworkUri, artworkBitmap, fallbackGradient) {
        gradientColors = artworkBitmap?.let { bitmap ->
            bitmap.extractArtworkGradientColors(fallbackGradient)
        } ?: fallbackGradient
    }

    Box(
        modifier = Modifier
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
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = artworkRequest,
                contentDescription = "Portada de ${content.playlist.name}",
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 16.dp)
                    .size(160.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp)),
                placeholder = painterResource(R.drawable.placeholder_playlist),
                error = painterResource(R.drawable.placeholder_playlist),
                fallback = painterResource(R.drawable.placeholder_playlist),
                onSuccess = { state ->
                    artworkBitmap = state.result.image.toBitmap()
                },
                contentScale = ContentScale.Crop,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = content.playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val isThisPlaylistActive = content.tracks.any { track ->
                    track.id == playbackState.currentTrack?.id
                }
                val isPlayingThis = isThisPlaylistActive &&
                    (playbackState.status == PlaybackStatus.Playing ||
                        playbackState.status == PlaybackStatus.Buffering)

                FilledIconButton(
                    onClick = {
                        if (isThisPlaylistActive) {
                            onTogglePlayback()
                        } else {
                            content.tracks.firstOrNull()?.let { firstTrack ->
                                onPlay(firstTrack, content.tracks)
                            }
                        }
                    },
                    enabled = content.tracks.isNotEmpty(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = if (isPlayingThis) {
                            painterResource(R.drawable.ic_pause)
                        } else {
                            painterResource(R.drawable.ic_play)
                        },
                        contentDescription = if (isPlayingThis) "Pausar" else "Reproducir playlist",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            if (content.tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Esta playlist está vacía.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                ) {
                    items(content.tracks, key = Track::id) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(track, content.tracks) }
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    track.artist.name,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onTrackOptions(track) }) {
                                Icon(
                                    painterResource(R.drawable.ic_options),
                                    "Opciones de ${track.title}",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
