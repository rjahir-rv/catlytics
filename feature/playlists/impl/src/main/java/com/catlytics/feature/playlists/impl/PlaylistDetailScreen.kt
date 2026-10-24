package com.catlytics.feature.playlists.impl

import com.catlytics.core.designsystem.layout.deferredPadding
import com.catlytics.core.designsystem.layout.plusHorizontal
import com.catlytics.core.designsystem.layout.withDeferredBottom
import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.catlytics.core.designsystem.component.ArtworkGradientBackground
import com.catlytics.core.designsystem.component.ArtworkGradientColors
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import com.catlytics.core.designsystem.component.SearchCollapsibleHeader
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.isSearching
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.extractArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberFallbackArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.NowPlaying
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.core.navigation.ViewModelStoreScope
import com.catlytics.core.designsystem.R as DsR
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistDetailScreen(
    uiState: PlaylistDetailUiState,
    nowPlaying: NowPlaying,
    allTracks: List<Track>,
    onPlay: (Track, List<Track>) -> Unit,
    onPlayShuffled: (List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onRemoveSelected: (List<String>) -> Unit = {},
    onTogglePlayback: () -> Unit,
    onSaveDetails: (String, String, String?, Boolean, () -> Unit) -> Unit,
    onSaveOrder: (List<String>, () -> Unit) -> Unit,
    onAddTracks: (List<String>) -> Unit,
    onDelete: () -> Unit,
    onExportM3u: () -> Unit = {},
    onTopBarColorChange: (Color) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (uiState) {
        PlaylistDetailUiState.Loading -> {
            PlaylistDetailSkeleton(topPadding = { scaffoldContentPadding.calculateTopPadding() })
            return
        }
        PlaylistDetailUiState.NotFound -> {
            CatlyticsEmptyState(
                message = stringResource(R.string.playlist_detail_not_found),
                messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    var editName by remember(playlist.id) { mutableStateOf(playlist.name) }
    var editDescription by remember(playlist.id) { mutableStateOf(playlist.description) }
    var editArtworkUri by remember(playlist.id) { mutableStateOf(playlist.artworkUri) }
    var artworkChanged by remember(playlist.id) { mutableStateOf(false) }

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
    val hidePlaylistHeader = shouldHidePlaylistHeader(searchFocused, searchQuery)
    val listState = rememberLazyListState()
    // bottomPadding ya incluye el teclado; se lee al medir/colocar (no al componer) para que su
    // animación no recomponga toda la pantalla.
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
    // Sin portada propia el fondo toma el color de identidad de la playlist (el mismo de su
    // portada generada); con portada se espera a extraer la paleta de la imagen.
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val surface = colorScheme.surface
    val identityGradient = remember(playlist.id, playlist.name, isDark, surface) {
        val base = playlistFallbackGradient(
            isLiked = playlist.id == LIKED_PLAYLIST_ID,
            seed = playlist.name,
            isDark = isDark,
            surface = surface,
        )
        ArtworkGradientColors(
            start = lerp(base.start, surface, IDENTITY_SURFACE_BLEND),
            center = lerp(base.center, surface, IDENTITY_SURFACE_BLEND),
            end = surface,
        )
    }
    val noArtworkGradient = if (playlist.artworkUri != null) fallbackGradient else identityGradient
    var artworkBitmap by remember(playlist.artworkUri) { mutableStateOf<Bitmap?>(null) }
    var gradientColors by remember { mutableStateOf(noArtworkGradient) }
    val animatedGradientColors = animateArtworkGradientColors(
        target = gradientColors,
        labelPrefix = "PlaylistDetailGradient",
    )

    // Se reporta el color destino; la barra superior lo anima por su cuenta.
    LaunchedEffect(gradientColors.start) {
        onTopBarColorChange(gradientColors.start)
    }
    LaunchedEffect(playlist.artworkUri, artworkBitmap, fallbackGradient, noArtworkGradient) {
        gradientColors = artworkBitmap?.extractArtworkGradientColors(
            fallback = fallbackGradient,
            surfaceBlend = PLAYLIST_ARTWORK_SURFACE_BLEND,
        ) ?: noArtworkGradient
    }
    var entranceDone by rememberSaveable(playlist.id) { mutableStateOf(false) }
    LaunchedEffect(playlist.id) {
        delay(ENTRANCE_SETTLE_MILLIS)
        entranceDone = true
    }
    LaunchedEffect(playlist.name, playlist.description, playlist.artworkUri) {
        if (!showEditSheet) {
            editName = playlist.name
            editDescription = playlist.description
            editArtworkUri = playlist.artworkUri
            artworkChanged = false
        }
    }
    LaunchedEffect(hidePlaylistHeader) {
        if (hidePlaylistHeader) {
            listState.scrollToItem(0)
        }
    }

    val selectionState = rememberTrackSelectionState()
    val selection = selectionState.value
    val isLikedPlaylist = playlist.id == LIKED_PLAYLIST_ID

    ArtworkGradientBackground(colors = animatedGradientColors) {
        TrackSelectionHost(
            selection = selection,
            onSelectionChange = { selectionState.value = it },
            visibleIds = remember(displayedTracks) { displayedTracks.map(Track::id) },
            selectedTracks = remember(selection, content.tracks) {
                selection.selectedTracks(content.tracks)
            },
            likedTrackIds = likedTrackIds,
            currentTrackId = nowPlaying.trackId,
            topInset = { scaffoldContentPadding.calculateTopPadding() },
            bottomInset = bottomPadding,
            onAction = onTrackSelectionAction,
            showRemove = !customOrdering,
            removeUnlike = isLikedPlaylist,
            onRemove = {
                onRemoveSelected(selection.selectedTracks(content.tracks).map(Track::id))
                selectionState.value = selection.clear()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalInset =
                ((maxWidth - CatlyticsLayoutTokens.ListContentMaxWidth) / 2).coerceAtLeast(0.dp)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollConnection),
                contentPadding = deferredPadding(
                    top = {
                        scaffoldContentPadding.calculateTopPadding() +
                            if (searchVisible && content.tracks.isNotEmpty() && !customOrdering && !selection.active) {
                                72.dp
                            } else if (selection.active) {
                                72.dp
                            } else {
                                0.dp
                            }
                    },
                ).plusHorizontal(horizontalInset).withDeferredBottom {
                    bottomPadding() + 20.dp + if (selection.active) 72.dp else 0.dp
                },
            ) {
                item(key = "playlist-header") {
                    SearchCollapsibleHeader(isSearching = hidePlaylistHeader) {
                        PlaylistHeader(
                            playlist = playlist,
                            tracks = content.tracks,
                            artworkRequest = artworkRequest,
                            nowPlaying = nowPlaying,
                            customOrdering = customOrdering,
                            onArtworkLoaded = { artworkBitmap = it },
                            onPlay = onPlay,
                            onPlayShuffled = onPlayShuffled,
                            onTogglePlayback = onTogglePlayback,
                            onAddTracksClick = { showAddTracksSheet = true },
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
                                    onExportM3u = onExportM3u,
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
                }

                if (content.tracks.isEmpty()) {
                    item(key = "empty") {
                        CatlyticsEmptyState(
                            message = stringResource(R.string.playlist_detail_empty),
                            mascotSize = 112.dp,
                            fillMaxSize = false,
                            modifier = Modifier.padding(vertical = 16.dp),
                            action = {
                                Button(onClick = { showAddTracksSheet = true }) {
                                    Icon(
                                        painter = painterResource(DsR.drawable.ic_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.playlist_detail_add_tracks),
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            },
                        )
                    }
                } else if (displayedTracks.isEmpty()) {
                    item(key = "no-results") {
                        CatlyticsEmptyState(
                            message = stringResource(R.string.playlist_detail_no_search_results),
                            messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            mascotSize = 120.dp,
                            fillMaxSize = false,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                } else {
                    itemsIndexed(displayedTracks, key = { _, track -> track.id }) { index, track ->
                        PlaylistTrackRow(
                            modifier = Modifier
                                .animateItem()
                                .staggeredEntrance(
                                    index = index,
                                    animate = !entranceDone && index < ENTRANCE_MAX_STAGGERED_ITEMS,
                                ),
                            track = track,
                            customOrdering = customOrdering,
                            isCurrent = track.id == nowPlaying.trackId,
                            isPlaying = track.id == nowPlaying.trackId &&
                                nowPlaying.isPlaying,
                            onClick = {
                                if (selection.active && !customOrdering) {
                                    selectionState.value = selection.onTrackLongClick(track.id)
                                } else {
                                    onPlay(track, content.tracks)
                                }
                            },
                            onOptions = { onTrackOptions(track) },
                            onMove = { direction ->
                                customTracks = customTracks.moveTrack(track.id, direction)
                            },
                            selected = track.id in selection.selectedIds,
                            selectionActive = selection.active,
                            onLongClick = {
                                if (!customOrdering) {
                                    selectionState.value = selection.onTrackLongClick(track.id)
                                }
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = searchVisible && content.tracks.isNotEmpty() && !customOrdering && !selection.active,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = slideInVertically { -it } + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                PlaylistTrackSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onFocusChange = { searchFocused = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            deferredPadding(
                                start = 20.dp,
                                top = { scaffoldContentPadding.calculateTopPadding() + 8.dp },
                                end = 20.dp,
                                bottom = { 8.dp },
                            ),
                        ),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, -(bottomPadding() + 8.dp).roundToPx()) },
            )
        }
        }
    }

    PlaylistDetailOverlays(
        playlistName = playlist.displayName(),
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
            selectionState.value = selection.clear()
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

    ViewModelStoreScope(key = playlist.id.takeIf { showAddSheet }) {
        AddToPlaylistSheet(
            source = PlaylistSource.TrackCollectionSource(
                title = playlist.displayName(),
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
                showAddTracksSheet = false
                onAddTracks(selectedTrackIds)
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
        DeletePlaylistDialog(
            playlistName = playlistName,
            message = stringResource(R.string.playlist_detail_delete_message),
            onDismiss = onDismissDelete,
            onConfirm = onDelete,
        )
    }
}

internal fun shouldHidePlaylistHeader(
    searchFocused: Boolean,
    searchQuery: String,
): Boolean = isSearching(isSearchActive = searchFocused, searchQuery = searchQuery)

private const val PLAYLIST_ARTWORK_SURFACE_BLEND = 0.32f
private const val IDENTITY_SURFACE_BLEND = 0.3f
private const val ENTRANCE_SETTLE_MILLIS = 1_200L
private const val ENTRANCE_MAX_STAGGERED_ITEMS = 12
