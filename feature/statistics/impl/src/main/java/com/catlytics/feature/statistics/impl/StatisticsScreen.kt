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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.WeeklyStats
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                        totals = state.totals,
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
    totals: ListeningTotals,
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
                "$minutes min"
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

        item {
            ListeningTotalsRow(totals)
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
            item {
                WeeklyActivityChart(stats.dailyListening)
            }

            item {
                WeeklySummary(stats)
            }

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
private fun ListeningTotalsRow(totals: ListeningTotals) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ListeningTotalItem(
            label = "Canciones",
            value = totals.trackCount,
            modifier = Modifier.weight(1f),
        )
        ListeningTotalItem(
            label = "Artistas",
            value = totals.artistCount,
            modifier = Modifier.weight(1f),
        )
        ListeningTotalItem(
            label = "Álbumes",
            value = totals.albumCount,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ListeningTotalItem(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeeklyActivityChart(dailyListening: List<com.catlytics.core.model.DailyListeningStat>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val coroutineScope = rememberCoroutineScope()
    val markerVisible = remember { mutableStateOf(false) }
    val hideMarkerJob = remember { mutableStateOf<Job?>(null) }
    val dailyMinutes = remember(dailyListening) {
        List(7) { index ->
            dailyListening.firstOrNull { it.dayOfWeek == index + 1 }
                ?.totalListenedMillis
                ?.div(60_000f)
                ?: 0f
        }
    }

    LaunchedEffect(dailyMinutes) {
        modelProducer.runTransaction {
            columnModel { series(y = dailyMinutes) }
        }
    }

    val markerLabel = rememberTextComponent(
        style = TextStyle(color = MaterialTheme.colorScheme.onPrimary),
        padding = Insets(horizontal = 8.dp, vertical = 4.dp),
        background = rememberShapeComponent(
            fill = Fill(MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(8.dp),
        ),
    )
    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = remember(markerVisible.value) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                if (!markerVisible.value) return@ValueFormatter ""
                val minutes = (targets.firstOrNull() as? ColumnCartesianLayerMarkerTarget)
                    ?.columns
                    ?.firstOrNull()
                    ?.entry
                    ?.y
                    ?: 0.0
                "${minutes.toInt()} min"
            }
        },
        labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
    )
    val markerController = remember {
        TimedMarkerController {
            markerVisible.value = true
            hideMarkerJob.value?.cancel()
            hideMarkerJob.value = coroutineScope.launch {
                delay(2_000)
                markerVisible.value = false
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Actividad semanal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Minutos escuchados por día",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    marker = marker,
                    markerController = markerController,
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 12.dp),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private class TimedMarkerController(
    private val onMarkerTapped: () -> Unit,
) : CartesianMarkerController {
    override val acceptsLongPress = false

    override fun shouldAcceptInteraction(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>,
    ) = interaction is Interaction.Tap && targets.isNotEmpty()

    override fun shouldShowMarker(
        interaction: Interaction,
        targets: List<CartesianMarker.Target>,
    ): Boolean {
        onMarkerTapped()
        return true
    }
}

@Composable
private fun WeeklySummary(stats: WeeklyStats) {
    val favoriteArtist = stats.topArtists.firstOrNull()?.name ?: "Sin datos"
    val favoriteTrack = stats.topTracks.firstOrNull()?.title ?: "Sin datos"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Resumen semanal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummaryValue("Artista favorito", favoriteArtist)
            SummaryValue("Canción más escuchada", favoriteTrack)
            SummaryValue("Reproducciones", stats.playCount.toString())
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
