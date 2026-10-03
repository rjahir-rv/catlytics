package com.catlytics.feature.playlists.impl

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistSummary
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.playlists.impl.R as PlaylistsR
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// La fila de controles (orden y modo de vista) va superpuesta sobre las listas,
// por lo que el contenido reserva su altura completa para no quedar tapado.
private val ControlsContentGap = 4.dp
private val ControlsContentInset = PlaylistsControlsHeight + ControlsContentGap

private const val ENTRANCE_SETTLE_MILLIS = 900L
private const val ENTRANCE_MAX_STAGGERED_ITEMS = 6

private enum class PlaylistsStateKind { Loading, Empty, NoResults, Content }

@Composable
internal fun PlaylistsScreen(
    uiState: PlaylistsUiState,
    viewMode: PlaylistViewMode,
    modifier: Modifier = Modifier,
    activePlaylist: ActivePlaylist? = null,
    onViewModeChange: (PlaylistViewMode) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreateClick: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetCover: (String, String?) -> Unit,
    onPlay: (String) -> Unit = {},
    onPlayShuffled: (String) -> Unit = {},
    onTogglePlayback: () -> Unit = {},
    searchQuery: String = "",
    sortDirection: SortDirection = SortDirection.Ascending,
    onSortDirectionChange: (SortDirection) -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    collapseFraction: () -> Float = { 0f },
) {
    var actionsFor by remember { mutableStateOf<PlaylistSummary?>(null) }
    var renaming by remember { mutableStateOf<PlaylistSummary?>(null) }
    var deleting by remember { mutableStateOf<PlaylistSummary?>(null) }
    var pendingCoverForId by remember { mutableStateOf<String?>(null) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val id = pendingCoverForId
        if (uri != null && id != null) {
            onSetCover(id, uri.toString())
        }
        pendingCoverForId = null
    }

    fun requestCoverChange(playlistId: String) {
        pendingCoverForId = playlistId
        coverPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val summaries = (uiState as? PlaylistsUiState.Content)?.playlists.orEmpty()
    val sorted = remember(summaries, searchQuery, sortDirection) {
        summaries.filterByQuery(searchQuery).sortedByDirection(sortDirection)
    }
    val stateKind = when {
        uiState is PlaylistsUiState.Loading -> PlaylistsStateKind.Loading
        summaries.isEmpty() -> PlaylistsStateKind.Empty
        sorted.isEmpty() -> PlaylistsStateKind.NoResults
        else -> PlaylistsStateKind.Content
    }
    val hasPlaylists = stateKind == PlaylistsStateKind.Content ||
        stateKind == PlaylistsStateKind.NoResults

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val coroutineScope = rememberCoroutineScope()
    var controlsHeightPx by remember { mutableIntStateOf(0) }
    val controlsAtTop by remember(viewMode) {
        derivedStateOf {
            if (viewMode == PlaylistViewMode.List) {
                listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
            } else {
                gridState.firstVisibleItemIndex == 0 &&
                    gridState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    // La entrada escalonada solo ocurre la primera vez; al volver desde el detalle no se repite.
    var entranceDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(stateKind == PlaylistsStateKind.Content) {
        if (stateKind == PlaylistsStateKind.Content) {
            delay(ENTRANCE_SETTLE_MILLIS)
            entranceDone = true
        }
    }

    fun selectSortDirection(direction: SortDirection) {
        if (direction == sortDirection) return
        coroutineScope.launch {
            when (viewMode) {
                PlaylistViewMode.List -> listState.scrollToItem(0)
                PlaylistViewMode.Mosaic -> gridState.scrollToItem(0)
            }
            onSortDirectionChange(direction)
        }
    }

    val contentPadding = PaddingValues(
        start = 20.dp,
        top = scaffoldContentPadding.calculateTopPadding() + ControlsContentInset,
        end = 20.dp,
        bottom = bottomPadding() + 104.dp,
    )

    fun playlistClick(summary: PlaylistSummary) = onPlaylistSelected(summary.playlist)
    fun playClick(summary: PlaylistSummary) {
        if (activePlaylist?.id == summary.playlist.id) {
            onTogglePlayback()
        } else {
            onPlay(summary.playlist.id)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = stateKind,
            modifier = Modifier.fillMaxSize(),
            label = "playlistsState",
        ) { kind ->
            when (kind) {
                PlaylistsStateKind.Loading -> PlaylistsSkeleton(
                    contentPadding = contentPadding,
                )

                PlaylistsStateKind.Empty -> CatlyticsEmptyState(
                    title = stringResource(PlaylistsR.string.playlists_empty_title),
                    message = stringResource(PlaylistsR.string.playlists_empty_message),
                    messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    action = {
                        Button(onClick = onCreateClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(PlaylistsR.string.playlists_create_action),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    },
                )

                PlaylistsStateKind.NoResults -> CatlyticsEmptyState(
                    message = stringResource(PlaylistsR.string.playlists_no_search_results),
                    messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    mascotSize = 160.dp,
                )

                PlaylistsStateKind.Content -> when (viewMode) {
                    PlaylistViewMode.List -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(sorted, key = { _, item -> item.playlist.id }) { index, summary ->
                            PlaylistListRow(
                                summary = summary,
                                isActive = activePlaylist?.id == summary.playlist.id,
                                isPlaying = activePlaylist?.isPlaying == true,
                                onClick = { playlistClick(summary) },
                                onLongClick = { actionsFor = summary },
                                onOptionsClick = { actionsFor = summary },
                                modifier = Modifier
                                    .animateItem()
                                    .staggeredEntrance(
                                        index = index,
                                        animate = !entranceDone && index < ENTRANCE_MAX_STAGGERED_ITEMS,
                                    ),
                            )
                        }
                    }

                    PlaylistViewMode.Mosaic -> LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        itemsIndexed(sorted, key = { _, item -> item.playlist.id }) { index, summary ->
                            PlaylistGridCard(
                                summary = summary,
                                isActive = activePlaylist?.id == summary.playlist.id,
                                isPlaying = activePlaylist?.isPlaying == true,
                                onClick = { playlistClick(summary) },
                                onLongClick = { actionsFor = summary },
                                onOptionsClick = { actionsFor = summary },
                                onPlayClick = { playClick(summary) },
                                modifier = Modifier
                                    .animateItem()
                                    .staggeredEntrance(
                                        index = index,
                                        animate = !entranceDone && index < ENTRANCE_MAX_STAGGERED_ITEMS,
                                    ),
                            )
                        }
                    }
                }
            }
        }

        if (hasPlaylists) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth()
                    .padding(top = scaffoldContentPadding.calculateTopPadding()),
            ) {
                PlaylistsControlsRow(
                    sortDirection = sortDirection,
                    viewMode = viewMode,
                    onSortSelected = ::selectSortDirection,
                    onViewModeChange = onViewModeChange,
                    modifier = Modifier
                        .onSizeChanged { controlsHeightPx = it.height }
                        .then(
                            if (controlsAtTop) {
                                Modifier
                            } else {
                                Modifier.graphicsLayer {
                                    val fraction = collapseFraction().coerceIn(0f, 1f)
                                    translationY = -fraction * controlsHeightPx
                                    alpha = 1f - fraction
                                }
                            },
                        )
                        .background(MaterialTheme.colorScheme.background),
                )
            }

            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                expanded = controlsAtTop,
                shape = CatlyticsCorners.Large,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = stringResource(
                            PlaylistsR.string.playlists_add_content_description,
                        ),
                    )
                },
                text = { Text(stringResource(PlaylistsR.string.playlists_new_playlist)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = bottomPadding() + 20.dp,
                    ),
            )
        }
    }

    actionsFor?.let { target ->
        PlaylistActionsSheet(
            summary = target,
            onDismiss = { actionsFor = null },
            onPlay = { onPlay(target.playlist.id) },
            onShuffle = { onPlayShuffled(target.playlist.id) },
            onRename = { renaming = target },
            onChangeCover = { requestCoverChange(target.playlist.id) },
            onClearCover = { onSetCover(target.playlist.id, null) },
            onDelete = { deleting = target },
        )
    }
    renaming?.let { target ->
        val otherPlaylists = remember(summaries, target) {
            summaries.map { it.playlist }.filter { it.id != target.playlist.id }
        }
        NameDialog(
            title = stringResource(PlaylistsR.string.playlists_rename_title),
            initialName = target.playlist.displayName(),
            isNameTaken = { candidate -> isPlaylistNameTaken(candidate, otherPlaylists) },
            onDismiss = { renaming = null },
            onConfirm = { newName ->
                renaming = null
                onRename(target.playlist.id, newName)
            },
        )
    }
    deleting?.let { target ->
        DeletePlaylistDialog(
            playlistName = target.playlist.displayName(),
            onDismiss = { deleting = null },
            onConfirm = {
                deleting = null
                onDelete(target.playlist.id)
            },
        )
    }
}

private fun previewSummary(
    id: String,
    name: String,
    trackCount: Int,
    artworks: List<String> = emptyList(),
) = PlaylistSummary(
    playlist = Playlist(id = id, name = name, trackIds = List(trackCount) { "$id-$it" }),
    trackCount = trackCount,
    totalDurationMillis = trackCount * 210_000L,
    coverArtworkUris = artworks,
)

private val previewContent = PlaylistsUiState.Content(
    listOf(
        previewSummary("p1", "Focus", 12),
        previewSummary("p2", "Gym", 1),
        previewSummary("p3", "Road trip 2025", 0),
    ),
)

@Preview(showBackground = true, name = "List mode")
@Composable
private fun PlaylistsScreenListPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            uiState = previewContent,
            viewMode = PlaylistViewMode.List,
            activePlaylist = ActivePlaylist(id = "p1", isPlaying = true),
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreateClick = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Mosaic mode")
@Composable
private fun PlaylistsScreenMosaicPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            uiState = previewContent,
            viewMode = PlaylistViewMode.Mosaic,
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreateClick = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun PlaylistsScreenEmptyPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            uiState = PlaylistsUiState.Content(emptyList()),
            viewMode = PlaylistViewMode.List,
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreateClick = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun PlaylistsScreenLoadingPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            uiState = PlaylistsUiState.Loading,
            viewMode = PlaylistViewMode.List,
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreateClick = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
        )
    }
}
