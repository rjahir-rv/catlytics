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
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsTrackRow
import com.catlytics.core.model.Track
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
        subtitle = "${track.artist.name} · ${track.durationMillis.formatDuration()}",
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
                    contentDescription = "Arrastrar ${track.title}",
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
                        contentDescription = "Opciones de ${track.title}",
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
        placeholder = { Text("Buscar canciones") },
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), "Limpiar búsqueda")
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

internal fun playlistSummaryLabel(tracks: List<Track>): String {
    val countLabel = if (tracks.size == 1) "1 canción" else "${tracks.size} canciones"
    if (tracks.isEmpty()) return countLabel
    return "$countLabel · ${formatPlaylistTotalDuration(tracks.sumOf(Track::durationMillis))}"
}

internal fun formatPlaylistTotalDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis.coerceAtLeast(0L).milliseconds.inWholeSeconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours h $minutes min"
        hours > 0 -> "$hours h"
        minutes > 0 -> "$minutes min"
        else -> String.format(Locale.US, "0:%02d", seconds)
    }
}

private fun Long.formatDuration(): String {
    val totalSeconds = milliseconds.inWholeSeconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
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
