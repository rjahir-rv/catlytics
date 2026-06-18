package com.catlytics.app.ui.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.feature.playlists.impl.AddToPlaylistSheet

@Composable
internal fun CatlyticsAppSheets(
    trackOptionsRequest: TrackOptionsRequest?,
    likedTrackIds: Collection<String>,
    canAddTrackToQueue: (Track) -> Boolean,
    onDismissTrackOptions: () -> Unit,
    onAddTrackToPlaylist: (Track) -> Unit,
    onToggleTrackLiked: (Track) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onGoToAlbum: (Track) -> Unit,
    onGoToArtist: (Track) -> Unit,
    playlistSource: PlaylistSource?,
    playlistSheetSession: Int,
    onDismissPlaylistSheet: () -> Unit,
) {
    trackOptionsRequest?.let { request ->
        TrackOptionsSheet(
            track = request.track,
            isLiked = request.track.id in likedTrackIds,
            canAddToQueue = canAddTrackToQueue(request.track),
            canRemoveFromPlaylist = request.onRemoveFromPlaylist != null,
            onDismiss = onDismissTrackOptions,
            onAddToPlaylist = { onAddTrackToPlaylist(request.track) },
            onToggleLiked = { onToggleTrackLiked(request.track) },
            onAddToQueue = { onAddTrackToQueue(request.track) },
            onGoToAlbum = { onGoToAlbum(request.track) },
            onGoToArtist = { onGoToArtist(request.track) },
            onRemoveFromPlaylist = {
                onDismissTrackOptions()
                request.onRemoveFromPlaylist?.invoke()
            },
        )
    }

    playlistSource?.let { source ->
        key(playlistSheetSession) {
            AddToPlaylistSheet(source = source, onDismiss = onDismissPlaylistSheet)
        }
    }
}

internal data class TrackOptionsRequest(
    val track: Track,
    val onRemoveFromPlaylist: (() -> Unit)? = null,
)
