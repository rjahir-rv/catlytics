package com.catlytics.app.playback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catlytics.app.R
import com.catlytics.core.designsystem.component.CatlyticsMiniPlayer
import com.catlytics.core.designsystem.component.extractArtworkAccentColor
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/** The accent stays remembered while the mini player is hidden, so it doesn't flash on return. */
@Composable
internal fun MiniPlayerHost(
    isVisible: Boolean,
    track: Track?,
    status: PlaybackStatus,
    progress: () -> Float,
    isNavigationBarShown: Boolean,
    hazeState: HazeState,
    onTogglePlayback: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var accent by remember { mutableStateOf<Pair<String?, Color>?>(null) }
    if (!isVisible) return
    Column(modifier = Modifier.fillMaxWidth()) {
        track?.let { track ->
            CatlyticsMiniPlayer(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = MINI_PLAYER_MAX_WIDTH)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                    )
                    .padding(bottom = if (isNavigationBarShown) 0.dp else 8.dp),
                title = track.title.displayTrackTitle(),
                artist = track.artist.name.displayArtistName(),
                isPlaying = status == PlaybackStatus.Playing,
                isBuffering = status == PlaybackStatus.Buffering,
                progress = progress,
                onTogglePlayback = onTogglePlayback,
                onSkipPrevious = onSkipPrevious,
                onSkipNext = onSkipNext,
                onClick = onClick,
                hazeState = hazeState,
                accentColor = accent
                    ?.takeIf { (uri, _) -> uri == track.artworkUri && uri != null }
                    ?.second
                    ?: Color.Unspecified,
                artwork = { artworkModifier ->
                    PlaybackArtwork(
                        artworkUri = track.artworkUri,
                        seed = track.title,
                        contentDescription = stringResource(
                            R.string.app_content_description_artwork,
                            track.title.displayTrackTitle(),
                        ),
                        modifier = artworkModifier,
                        onSuccess = { bitmap ->
                            val artworkUri = track.artworkUri
                            coroutineScope.launch {
                                accent = artworkUri to bitmap.extractArtworkAccentColor()
                            }
                        },
                    )
                },
            )
        }
    }
}

private val MINI_PLAYER_MAX_WIDTH = 720.dp
