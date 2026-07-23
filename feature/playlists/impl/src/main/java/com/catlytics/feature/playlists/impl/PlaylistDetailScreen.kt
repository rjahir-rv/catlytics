package com.catlytics.feature.playlists.impl

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.catlytics.core.designsystem.component.ArtworkGradientBackground
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.extractArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberFallbackArtworkGradientColors
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistDetailScreen(
    uiState: PlaylistDetailUiState,
    playbackState: PlaybackState,
    allTracks: List<Track>,
    onPlay: (Track, List<Track>) -> Unit,
    onPlayShuffled: (List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onTogglePlayback: () -> Unit,
    onSaveDetails: (String, String, String?, Boolean, () -> Unit) -> Unit,
    onSaveOrder: (List<String>, () -> Unit) -> Unit,
    onAddTracks: (List<String>, () -> Unit) -> Unit,
    onDelete: () -> Unit,
    onTopBarColorChange: (Color) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    when (uiState) {
        PlaylistDetailUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        PlaylistDetailUiState.NotFound -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Playlist no disponible.")
            }
            return
        }
        is PlaylistDetailUiState.Success -> Unit
    }

    val content = uiState.content
    val playlist = content.playlist
    var searchQuery by rememberSaveable(playlist.id) { mutableStateOf("") }
    var searchVisible by rememberSaveable(playlist.id) { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    var optionsExpanded by remember { mutableStateOf(false) }
    var showOrderSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddTracksSheet by remember { mutableStateOf(false) }
    var customOrdering by remember { mutableStateOf(false) }
    var customTracks by remember { mutableStateOf(emptyList<Track>()) }
    var editName by remember { mutableStateOf(playlist.name) }
    var editDescription by remember { mutableStateOf(playlist.description) }
    var editArtworkUri by remember { mutableStateOf(playlist.artworkUri) }
    var artworkChanged by remember { mutableStateOf(false) }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            editArtworkUri = uri.toString()
            artworkChanged = true
        }
    }

    val filteredTracks = remember(content.tracks, searchQuery) {
        content.tracks.filterPlaylistTracksByQuery(searchQuery)
    }
    val displayedTracks = if (customOrdering) customTracks else filteredTracks
    val listState = rememberLazyListState()
    val scrollConnection = remember(searchQuery, searchFocused, customOrdering) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && !customOrdering) {
                    when {
                        available.y > 1f -> searchVisible = true
                        available.y < -1f && searchQuery.isBlank() && !searchFocused -> {
                            searchVisible = false
                        }
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
        }
    }

    val platformContext = LocalPlatformContext.current
    val fallbackGradient = rememberFallbackArtworkGradientColors()
    val artworkRequest = remember(platformContext, playlist.artworkUri) {
        ImageRequest.Builder(platformContext)
            .data(playlist.artworkUri)
            .allowHardware(false)
            .build()
    }
    var artworkBitmap by remember(playlist.artworkUri) { mutableStateOf<Bitmap?>(null) }
    var gradientColors by remember { mutableStateOf(fallbackGradient) }
    val animatedGradientColors = animateArtworkGradientColors(
        target = gradientColors,
        labelPrefix = "PlaylistDetailGradient",
    )

    LaunchedEffect(animatedGradientColors.start) {
        onTopBarColorChange(animatedGradientColors.start)
    }
    LaunchedEffect(playlist.artworkUri, artworkBitmap, fallbackGradient) {
        gradientColors = artworkBitmap?.extractArtworkGradientColors(
            fallback = fallbackGradient,
            surfaceBlend = PLAYLIST_ARTWORK_SURFACE_BLEND,
        ) ?: fallbackGradient
    }
    LaunchedEffect(playlist.name, playlist.description, playlist.artworkUri) {
        if (!showEditSheet) {
            editName = playlist.name
            editDescription = playlist.description
            editArtworkUri = playlist.artworkUri
            artworkChanged = false
        }
    }

    ArtworkGradientBackground(colors = animatedGradientColors) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = searchVisible && content.tracks.isNotEmpty() && !customOrdering,
                enter = slideInVertically { -it } + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                PlaylistTrackSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onFocusChange = { searchFocused = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 8.dp),
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollConnection),
                contentPadding = PaddingValues(bottom = bottomPadding() + 20.dp),
            ) {
                item(key = "playlist-header") {
                    PlaylistHeader(
                        playlist = playlist,
                        tracks = content.tracks,
                        artworkRequest = artworkRequest,
                        playbackState = playbackState,
                        customOrdering = customOrdering,
                        onArtworkLoaded = { artworkBitmap = it },
                        onPlay = onPlay,
                        onPlayShuffled = onPlayShuffled,
                        onTogglePlayback = onTogglePlayback,
                        onOptionsClick = { optionsExpanded = true },
                        optionsMenu = {
                            PlaylistOptionsMenu(
                                expanded = optionsExpanded,
                                canEdit = playlist.id != LIKED_PLAYLIST_ID,
                                onDismiss = { optionsExpanded = false },
                                onOrder = { showOrderSheet = true },
                                onEdit = {
                                    editName = playlist.name
                                    editDescription = playlist.description
                                    editArtworkUri = playlist.artworkUri
                                    artworkChanged = false
                                    showEditSheet = true
                                },
                                onAddTracks = { showAddTracksSheet = true },
                                onAddToPlaylist = { showAddSheet = true },
                                onDelete = { showDeleteDialog = true },
                            )
                        },
                        onCancelOrdering = {
                            customOrdering = false
                            customTracks = emptyList()
                        },
                        onSaveOrdering = {
                            onSaveOrder(customTracks.map(Track::id)) {
                                customOrdering = false
                                customTracks = emptyList()
                            }
                        },
                    )
                }

                if (content.tracks.isEmpty()) {
                    item(key = "empty") {
                        PlaylistMessage("Esta playlist está vacía.")
                    }
                } else if (displayedTracks.isEmpty()) {
                    item(key = "no-results") {
                        PlaylistMessage(
                            text = "No encontramos canciones que coincidan con tu búsqueda.",
                            secondary = true,
                        )
                    }
                } else {
                    items(displayedTracks, key = Track::id) { track ->
                        PlaylistTrackRow(
                            track = track,
                            customOrdering = customOrdering,
                            onClick = { onPlay(track, content.tracks) },
                            onOptions = { onTrackOptions(track) },
                            onMove = { direction ->
                                customTracks = customTracks.moveTrack(track.id, direction)
                            },
                        )
                    }
                }
            }
        }
    }

    PlaylistDetailOverlays(
        playlistName = playlist.name,
        showOrderSheet = showOrderSheet,
        showEditSheet = showEditSheet,
        showDeleteDialog = showDeleteDialog,
        editName = editName,
        editDescription = editDescription,
        editArtworkUri = editArtworkUri,
        onDismissOrder = { showOrderSheet = false },
        onCustomOrder = {
            showOrderSheet = false
            searchQuery = ""
            searchVisible = false
            customTracks = content.tracks
            customOrdering = true
        },
        onAlphabeticalOrder = {
            showOrderSheet = false
            onSaveOrder(content.tracks.alphabeticalOrder().map(Track::id)) {}
        },
        onRandomOrder = {
            showOrderSheet = false
            onSaveOrder(content.tracks.shuffledOrder().map(Track::id)) {}
        },
        onNameChange = { editName = it },
        onDescriptionChange = { editDescription = it },
        onChooseArtwork = {
            coverPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onRemoveArtwork = {
            editArtworkUri = null
            artworkChanged = true
        },
        onDismissEdit = { showEditSheet = false },
        onSaveEdit = {
            onSaveDetails(
                editName,
                editDescription,
                editArtworkUri,
                artworkChanged,
            ) { showEditSheet = false }
        },
        onDismissDelete = { showDeleteDialog = false },
        onDelete = {
            showDeleteDialog = false
            onDelete()
        },
    )

    if (showAddSheet) {
        AddToPlaylistSheet(
            source = PlaylistSource.TrackCollectionSource(
                title = playlist.name,
                artworkUri = playlist.artworkUri,
                trackIds = content.tracks.map(Track::id),
            ),
            excludedPlaylistIds = setOf(playlist.id),
            allowCreate = false,
            onDismiss = { showAddSheet = false },
        )
    }
    if (showAddTracksSheet) {
        AddTracksToPlaylistSheet(
            tracks = allTracks,
            existingTrackIds = playlist.trackIds.toSet(),
            onDismiss = { showAddTracksSheet = false },
            onAdd = { selectedTrackIds ->
                onAddTracks(selectedTrackIds) { showAddTracksSheet = false }
            },
        )
    }
}

@Composable
private fun PlaylistMessage(text: String, secondary: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (secondary) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = if (secondary) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun PlaylistDetailOverlays(
    playlistName: String,
    showOrderSheet: Boolean,
    showEditSheet: Boolean,
    showDeleteDialog: Boolean,
    editName: String,
    editDescription: String,
    editArtworkUri: String?,
    onDismissOrder: () -> Unit,
    onCustomOrder: () -> Unit,
    onAlphabeticalOrder: () -> Unit,
    onRandomOrder: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onChooseArtwork: () -> Unit,
    onRemoveArtwork: () -> Unit,
    onDismissEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onDismissDelete: () -> Unit,
    onDelete: () -> Unit,
) {
    if (showOrderSheet) {
        PlaylistOrderSheet(
            onDismiss = onDismissOrder,
            onCustom = onCustomOrder,
            onAlphabetical = onAlphabeticalOrder,
            onRandom = onRandomOrder,
        )
    }
    if (showEditSheet) {
        EditPlaylistSheet(
            name = editName,
            description = editDescription,
            artworkUri = editArtworkUri,
            onNameChange = onNameChange,
            onDescriptionChange = onDescriptionChange,
            onChooseArtwork = onChooseArtwork,
            onRemoveArtwork = onRemoveArtwork,
            onDismiss = onDismissEdit,
            onSave = onSaveEdit,
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Eliminar $playlistName") },
            text = {
                Text(
                    "Esta acción no se puede deshacer. Las canciones permanecerán en el dispositivo.",
                )
            },
            confirmButton = {
                TextButton(onClick = onDelete) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancelar") }
            },
        )
    }
}

private const val PLAYLIST_ARTWORK_SURFACE_BLEND = 0.32f
