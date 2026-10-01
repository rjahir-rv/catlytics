package com.catlytics.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

@Composable
fun CatlyticsMiniPlayer(
    modifier: Modifier = Modifier,
    title: String,
    artist: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float = 0f,
    onTogglePlayback: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    hazeState: HazeState? = null,
    accentColor: Color = Color.Unspecified,
    artwork: @Composable (Modifier) -> Unit = { artworkModifier ->
        Image(
            painter = painterResource(id = R.drawable.placeholder_track),
            contentDescription = null,
            modifier = artworkModifier,
            contentScale = ContentScale.Crop,
        )
    },
) {
    val containerShape = RoundedCornerShape(20.dp)
    // Solo se anima el acento de la portada; los colores del tema ya llegan animados y
    // animarlos otra vez haría que el mini se desfasara al cambiar entre claro y oscuro.
    var lastAccent by remember { mutableStateOf(Color.Unspecified) }
    if (accentColor.isSpecified) lastAccent = accentColor
    val animatedAccent by animateColorAsState(
        targetValue = if (lastAccent.isSpecified) lastAccent else MaterialTheme.colorScheme.primary,
        animationSpec = tween(MINI_PLAYER_COLOR_ANIMATION_MILLIS),
        label = "miniPlayerAccent",
    )
    val accentWeight by animateFloatAsState(
        targetValue = if (accentColor.isSpecified) 1f else 0f,
        animationSpec = tween(MINI_PLAYER_COLOR_ANIMATION_MILLIS),
        label = "miniPlayerAccentWeight",
    )
    val tintColor = lerp(
        MaterialTheme.colorScheme.surfaceContainer,
        animatedAccent,
        MINI_PLAYER_ACCENT_BLEND * accentWeight,
    )
    val progressColor = lerp(MaterialTheme.colorScheme.primary, animatedAccent, accentWeight)
    val backgroundModifier = if (hazeState != null) {
        Modifier.hazeBlur(
            input = HazeInput.Sources(hazeState),
            style = HazeBlurStyle {
                blurRadius(28.dp)
                noiseFactor(0.05f)
                colorEffects(listOf(HazeColorEffect.tint(tintColor.copy(alpha = 0.72f))))
                fallbackColorEffect(HazeColorEffect.tint(tintColor.copy(alpha = 0.92f)))
            },
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(containerShape)
            .then(backgroundModifier)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = containerShape,
            )
            .animateContentSize(),
        onClick = onClick,
        shape = containerShape,
        color = if (hazeState != null) Color.Transparent else tintColor.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        val progressContentDescription = stringResource(
            R.string.ds_mini_player_progress_content_description,
        )
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    artwork(Modifier.matchParentSize())
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_back),
                            contentDescription = stringResource(R.string.ds_action_previous),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = onTogglePlayback,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        ),
                    ) {
                        Icon(
                            painter = if (isPlaying || isBuffering) {
                                painterResource(R.drawable.ic_pause)
                            } else {
                                painterResource(id = R.drawable.ic_play)
                            },
                            contentDescription = if (isPlaying) {
                                stringResource(R.string.ds_action_pause)
                            } else {
                                stringResource(R.string.ds_action_play)
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_next),
                            contentDescription = stringResource(R.string.ds_action_next),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .semantics {
                        contentDescription = progressContentDescription
                    },
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

private const val MINI_PLAYER_ACCENT_BLEND = 0.3f
private const val MINI_PLAYER_COLOR_ANIMATION_MILLIS = 600
