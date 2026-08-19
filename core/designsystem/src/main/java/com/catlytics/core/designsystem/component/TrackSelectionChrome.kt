package com.catlytics.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.core.model.TrackSelectionSnapshot

@Composable
fun rememberTrackSelectionState(): MutableState<TrackSelectionSnapshot> =
    rememberSaveable(saver = TrackSelectionSnapshotStateSaver) {
        mutableStateOf(TrackSelectionSnapshot())
    }

@Composable
fun TrackSelectionHost(
    selection: TrackSelectionSnapshot,
    onSelectionChange: (TrackSelectionSnapshot) -> Unit,
    visibleIds: List<String>,
    selectedTracks: List<Track>,
    likedTrackIds: Set<String>,
    currentTrackId: String?,
    topInset: Dp,
    bottomInset: Dp,
    onAction: (TrackSelectionAction) -> Unit,
    modifier: Modifier = Modifier,
    showRemove: Boolean = false,
    removeUnlike: Boolean = false,
    onRemove: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    BackHandler(enabled = selection.active) {
        onSelectionChange(selection.clear())
    }
    val actionsEnabled = selection.selectedCount > 0
    val likeRemoves = selection.allSelectedAreLiked(likedTrackIds)
    val queueEnabled = actionsEnabled &&
        currentTrackId != null &&
        selectedTracks.any { it.id != currentTrackId }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarPadding = maxOf(topInset, statusBarPadding + SelectionTopBarClearance)

    Box(modifier = modifier) {
        content()
        AnimatedVisibility(
            visible = selection.active,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            TrackSelectionTopBar(
                selectedCount = selection.selectedCount,
                onClose = { onSelectionChange(selection.clear()) },
                onSelectVisible = { onSelectionChange(selection.selectVisible(visibleIds)) },
                modifier = Modifier.padding(top = topBarPadding),
            )
        }
        AnimatedVisibility(
            visible = selection.active,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            TrackSelectionActionBar(
                enabled = actionsEnabled,
                likeRemoves = likeRemoves,
                queueEnabled = queueEnabled,
                showLike = !removeUnlike,
                showRemove = showRemove,
                removeUnlike = removeUnlike,
                onAddToPlaylist = {
                    if (actionsEnabled) onAction(TrackSelectionAction.AddToPlaylist(selectedTracks))
                },
                onToggleLiked = {
                    if (!actionsEnabled) return@TrackSelectionActionBar
                    onAction(
                        if (likeRemoves) {
                            TrackSelectionAction.Unlike(selectedTracks)
                        } else {
                            TrackSelectionAction.Like(selectedTracks)
                        },
                    )
                },
                onPlayNext = {
                    if (queueEnabled) onAction(TrackSelectionAction.PlayNext(selectedTracks))
                },
                onAddToQueue = {
                    if (queueEnabled) onAction(TrackSelectionAction.AddToQueue(selectedTracks))
                },
                onRemove = onRemove,
                modifier = Modifier.padding(bottom = bottomInset),
            )
        }
    }
}

@Composable
private fun TrackSelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectVisible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Salir de la selección",
                )
            }
            Text(
                text = if (selectedCount == 1) "1 seleccionada" else "$selectedCount seleccionadas",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(onClick = onSelectVisible) {
                Text("Seleccionar todo")
            }
        }
    }
}

@Composable
private fun TrackSelectionActionBar(
    enabled: Boolean,
    likeRemoves: Boolean,
    queueEnabled: Boolean,
    showLike: Boolean,
    showRemove: Boolean,
    removeUnlike: Boolean,
    onAddToPlaylist: () -> Unit,
    onToggleLiked: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAddToPlaylist, enabled = enabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_playlist),
                    contentDescription = "Agregar a playlist",
                )
            }
            if (showLike) {
                IconButton(onClick = onToggleLiked, enabled = enabled) {
                    Icon(
                        painter = painterResource(
                            if (likeRemoves) R.drawable.ic_favorite_fill else R.drawable.ic_favorite,
                        ),
                        contentDescription = if (likeRemoves) {
                            "Quitar de Tus me gusta"
                        } else {
                            "Guardar en Tus me gusta"
                        },
                    )
                }
            }
            IconButton(onClick = onPlayNext, enabled = queueEnabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_next),
                    contentDescription = "Reproducir siguiente",
                )
            }
            IconButton(onClick = onAddToQueue, enabled = queueEnabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Agregar a la cola",
                )
            }
            if (showRemove) {
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = if (removeUnlike) {
                            "Quitar de Tus me gusta"
                        } else {
                            "Quitar de playlist"
                        },
                    )
                }
            }
        }
    }
}

private val SelectionTopBarClearance = 64.dp

private val TrackSelectionSnapshotStateSaver = Saver<MutableState<TrackSelectionSnapshot>, ArrayList<Any>>(
    save = { state ->
        arrayListOf<Any>(state.value.active).apply { addAll(state.value.selectedIds) }
    },
    restore = { saved ->
        mutableStateOf(
            TrackSelectionSnapshot(
                active = saved[0] as Boolean,
                selectedIds = saved.drop(1).map { it as String }.toSet(),
            ),
        )
    },
)
