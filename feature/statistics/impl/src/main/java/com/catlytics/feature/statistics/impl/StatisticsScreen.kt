package com.catlytics.feature.statistics.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.crossfade
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import com.catlytics.core.model.WeeklyStats
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp }
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is StatisticsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is StatisticsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ocurrió un error al cargar las estadísticas.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is StatisticsUiState.Success -> {
                    StatisticsContent(
                        weekOffset = state.weekOffset,
                        stats = state.stats,
                        onWeekSelected = { viewModel.selectWeek(it) },
                        bottomPadding = bottomPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    weekOffset: Int,
    stats: WeeklyStats,
    onWeekSelected: (Int) -> Unit,
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp }
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    val startText = remember(stats.weekStart) {
        dateFormatter.format(Instant.ofEpochMilli(stats.weekStart))
    }
    val endText = remember(stats.weekEnd) {
        // Subtract 1ms to show the date correctly as the end of the week, i.e. Sunday night
        dateFormatter.format(Instant.ofEpochMilli(stats.weekEnd - 1))
    }
    val dateRangeText = "$startText - $endText"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = bottomPadding() + 24.dp)
    ) {
        item {
            // Week selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = weekOffset == 0,
                    onClick = { onWeekSelected(0) },
                    label = { Text("Esta semana") }
                )
                FilterChip(
                    selected = weekOffset == -1,
                    onClick = { onWeekSelected(-1) },
                    label = { Text("Semana pasada") }
                )
            }
        }

        item {
            // Date range display
            Text(
                text = dateRangeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            // Total duration card
            val totalMinutes = stats.totalListenedMillis / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val formattedTime = if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes} min"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Tiempo total escuchado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 48.sp
                    )
                    if (hours > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Equivalente a $totalMinutes minutos de música",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (stats.topTracks.isEmpty() && stats.topArtists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No hay datos de reproducción",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Escucha música por más de 30 segundos para generar estadísticas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            if (stats.topTracks.isNotEmpty()) {
                item {
                    Text(
                        text = "Top Canciones",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                itemsIndexed(stats.topTracks) { index, track ->
                    TopTrackItem(rank = index + 1, track = track)
                }
            }

            if (stats.topArtists.isNotEmpty()) {
                item {
                    Text(
                        text = "Top Artistas",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                itemsIndexed(stats.topArtists) { index, artist ->
                    TopArtistItem(rank = index + 1, artist = artist)
                }
            }
        }
    }
}

@Composable
private fun TopTrackItem(
    rank: Int,
    track: TopTrack
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank indicator
        Text(
            text = "$rank",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(28.dp)
                .padding(end = 8.dp)
        )

        // Artwork
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                    .data(track.artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(R.drawable.placeholder_album),
                error = painterResource(R.drawable.placeholder_album),
                fallback = painterResource(R.drawable.placeholder_album),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Titles
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play Count
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${track.playCount}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "reproducciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun TopArtistItem(
    rank: Int,
    artist: TopArtist
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank indicator
        Text(
            text = "$rank",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(28.dp)
                .padding(end = 8.dp)
        )

        // Circle Artwork (artist style)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                    .data(artist.artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                placeholder = painterResource(R.drawable.placeholder_album),
                error = painterResource(R.drawable.placeholder_album),
                fallback = painterResource(R.drawable.placeholder_album),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name
        Text(
            text = artist.name,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Play Count
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${artist.playCount}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "reproducciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
