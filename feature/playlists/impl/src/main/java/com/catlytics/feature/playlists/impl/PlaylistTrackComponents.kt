package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsTrackRow
import com.catlytics.core.designsystem.format.TrackDurationFormat
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.Track
import com.catlytics.feature.playlists.impl.R as PlaylistsR
import java.text.Collator
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun PlaylistTrackRow(
    track: Track,
    customOrdering: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptions: () -> Unit,
    onMove: (Int) -> Unit,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val moveThresholdPx = with(density) { 48.dp.toPx() }
    var dragDistance by remember(track.id) { mutableFloatStateOf(0f) }

    CatlyticsTrackRow(
        title = track.title,
        subtitle = stringResource(
            PlaylistsR.string.playlist_detail_track_metadata,
            track.artist.name,
            TrackDurationFormat.format(track.durationMillis),
        ),
        artworkUri = track.artworkUri,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onClick,
        clickEnabled = !customOrdering || selectionActive,
        selected = selected,
        selectionActive = selectionActive && !customOrdering,
        onLongClick = onLongClick.takeUnless { customOrdering },
        trailing = {
            if (customOrdering) {
                Icon(
                    painter = painterResource(R.drawable.ic_item_selection),
                    contentDescription = stringResource(
                        PlaylistsR.string.playlist_detail_drag_content_description,
                        track.title,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(12.dp)
                        .pointerInput(track.id) {
                            detectDragGesturesAfterLongPress(
                                onDragEnd = { dragDistance = 0f },
                                onDragCancel = { dragDistance = 0f },
                            ) { change, dragAmount ->
                                change.consume()
                                dragDistance += dragAmount.y
                                if (abs(dragDistance) >= moveThresholdPx) {
                                    onMove(if (dragDistance > 0f) 1 else -1)
                                    dragDistance = 0f
                                }
                            }
                        },
                )
            } else {
                IconButton(onClick = onOptions) {
                    Icon(
                        painterResource(R.drawable.ic_options),
                        contentDescription = stringResource(
                            PlaylistsR.string.playlist_detail_track_options_content_description,
                            track.title,
                        ),
                    )
                }
            }
        },
        modifier = Modifier.padding(start = 20.dp, end = 12.dp),
    )
}

@Composable
internal fun PlaylistTrackSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(56.dp)
            .onFocusChanged { onFocusChange(it.isFocused) },
        placeholder = { Text(stringResource(PlaylistsR.string.playlist_detail_search_placeholder)) },
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        stringResource(PlaylistsR.string.playlists_clear_search_content_description),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )
}

internal fun List<Track>.alphabeticalOrder(): List<Track> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es")).apply {
        strength = Collator.PRIMARY
    }
    return sortedWith { first, second ->
        val titleComparison = collator.compare(first.title, second.title)
        if (titleComparison != 0) titleComparison else collator.compare(
            first.artist.name,
            second.artist.name,
        )
    }
}

internal fun List<Track>.shuffledOrder(random: Random = Random.Default): List<Track> =
    shuffled(random).let { shuffled ->
        if (size > 1 && shuffled == this) shuffled.drop(1) + shuffled.first() else shuffled
    }

internal fun List<Track>.moveTrack(trackId: String, direction: Int): List<Track> {
    val fromIndex = indexOfFirst { it.id == trackId }
    if (fromIndex == -1) return this
    val toIndex = (fromIndex + direction).coerceIn(indices)
    if (fromIndex == toIndex) return this
    return toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

internal fun toggleTrackSelection(
    selectedIds: Set<String>,
    trackId: String,
    existingTrackIds: Set<String>,
): Set<String> = when (trackId) {
    in existingTrackIds -> selectedIds
    in selectedIds -> selectedIds - trackId
    else -> selectedIds + trackId
}

internal fun List<Track>.selectedTrackIdsInLibraryOrder(selectedIds: Set<String>): List<String> =
    filter { it.id in selectedIds }.map(Track::id)

/** Nombre visible de la playlist; la playlist del sistema usa su recurso localizado. */
@Composable
internal fun Playlist.displayName(): String =
    if (id == LIKED_PLAYLIST_ID) {
        stringResource(PlaylistsR.string.playlist_liked_name)
    } else {
        name
    }

@Composable
internal fun playlistSummaryLabel(tracks: List<Track>): String {
    val countLabel = pluralStringResource(
        PlaylistsR.plurals.playlists_track_count,
        tracks.size,
        tracks.size,
    )
    if (tracks.isEmpty()) return countLabel
    return stringResource(
        PlaylistsR.string.playlist_detail_summary,
        countLabel,
        formatPlaylistTotalDuration(tracks.sumOf(Track::durationMillis)),
    )
}

/** Desglose puro de la duración total, para poder probarlo sin Compose. */
internal data class PlaylistDurationParts(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
)

internal fun playlistDurationParts(durationMillis: Long): PlaylistDurationParts {
    val totalSeconds = durationMillis.coerceAtLeast(0L).milliseconds.inWholeSeconds
    return PlaylistDurationParts(
        hours = totalSeconds / 3_600,
        minutes = (totalSeconds % 3_600) / 60,
        seconds = totalSeconds % 60,
    )
}

@Composable
internal fun formatPlaylistTotalDuration(durationMillis: Long): String {
    val parts = playlistDurationParts(durationMillis)
    return when {
        parts.hours > 0 && parts.minutes > 0 -> stringResource(
            PlaylistsR.string.playlist_detail_duration_hours_minutes,
            pluralStringResource(
                PlaylistsR.plurals.playlist_detail_duration_hours,
                parts.hours.toInt(),
                parts.hours,
            ),
            pluralStringResource(
                PlaylistsR.plurals.playlist_detail_duration_minutes,
                parts.minutes.toInt(),
                parts.minutes,
            ),
        )
        parts.hours > 0 -> pluralStringResource(
            PlaylistsR.plurals.playlist_detail_duration_hours,
            parts.hours.toInt(),
            parts.hours,
        )
        parts.minutes > 0 -> pluralStringResource(
            PlaylistsR.plurals.playlist_detail_duration_minutes,
            parts.minutes.toInt(),
            parts.minutes,
        )
        else -> TrackDurationFormat.format(durationMillis.coerceAtLeast(0L))
    }
}

internal fun List<Track>.filterPlaylistTracksByQuery(query: String): List<Track> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { track ->
        track.title.contains(normalizedQuery, ignoreCase = true) ||
            track.artist.name.contains(normalizedQuery, ignoreCase = true)
    }
}
