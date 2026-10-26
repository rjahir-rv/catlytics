package com.catlytics.feature.library.impl.artist

import android.graphics.Bitmap
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.layout.ArtworkHeaderLayout
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.component.CatlyticsMenuDivider
import com.catlytics.core.designsystem.component.CatlyticsMenuEntry
import com.catlytics.core.designsystem.component.CatlyticsMenuItem
import com.catlytics.core.model.ArtistContent
import com.catlytics.feature.library.impl.collection.CollectionPlaybackRow
import com.catlytics.feature.library.impl.collection.collectionMenuEntries
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.library.impl.R as LibraryR

@Composable
internal fun ArtistHeader(
    content: ArtistContent,
    artworkModel: Any?,
    onArtworkLoaded: (Bitmap) -> Unit,
    sortLabel: String?,
    playbackQueue: List<Track>,
    onPlayAll: () -> Unit,
    onPlayShuffled: () -> Unit,
    menuEntries: List<CatlyticsMenuEntry>,
    modifier: Modifier = Modifier,
) {
    ArtworkHeaderLayout(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        artwork = { isWide ->
            CatlyticsArtwork(
                model = artworkModel,
                seed = content.summary.artist.name,
                placeholderIcon = R.drawable.ic_artist,
                contentDescription = stringResource(
                    LibraryR.string.library_artist_image_content_description,
                    content.summary.artist.name.displayArtistName(),
                ),
                modifier = Modifier
                    .then(
                        if (isWide) {
                            Modifier.size(ArtistArtworkWideSize)
                        } else {
                            Modifier
                                .fillMaxWidth(0.52f)
                                .widthIn(max = ArtistArtworkMaxSize)
                        },
                    )
                    .aspectRatio(1f)
                    .clip(CircleShape),
                onSuccess = onArtworkLoaded,
                contentScale = ContentScale.Crop,
            )
        },
        details = {
            Text(
                text = content.summary.artist.name.displayArtistName(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val albumCountText = pluralStringResource(
                R.plurals.ds_album_count,
                content.summary.albumCount,
                content.summary.albumCount,
            )
            val trackCountText = pluralStringResource(
                R.plurals.ds_track_count,
                content.summary.trackCount,
                content.summary.trackCount,
            )
            Text(
                text = stringResource(
                    LibraryR.string.library_artist_metadata,
                    albumCountText,
                    trackCountText,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sortLabel?.let { label ->
                Text(
                    text = stringResource(LibraryR.string.library_collection_sorted_by, label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CollectionPlaybackRow(
                tracks = playbackQueue,
                onPlayShuffled = onPlayShuffled,
                onPlayAll = onPlayAll,
                menuEntries = menuEntries,
                menuContentDescription = stringResource(
                    R.string.ds_options_content_description,
                    content.summary.artist.name.displayArtistName(),
                ),
            )
        },
    )
}

private val ArtistArtworkMaxSize = 220.dp
private val ArtistArtworkWideSize = 180.dp

@Composable
internal fun artistMenuEntries(
    content: ArtistContent,
    tracks: List<Track>,
    likedTrackIds: Set<String>,
    section: ArtistDetailSection,
    aliasCount: Int,
    canRename: Boolean,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onSort: (ArtistDetailSection) -> Unit,
    onShowRename: () -> Unit,
    onChangeArtwork: () -> Unit,
    onRestoreArtwork: () -> Unit,
    onShowMergePicker: () -> Unit,
    onShowAliasManager: () -> Unit,
): List<CatlyticsMenuEntry> = buildList {
    addAll(
        collectionMenuEntries(
            tracks = tracks,
            likedTrackIds = likedTrackIds,
            onTrackSelectionAction = onTrackSelectionAction,
            onAddToPlaylist = { onAddToPlaylist(PlaylistSource.ArtistSource(content.summary.artist.id)) },
        ),
    )
    // Sorting follows the visible tab, so the menu offers the list the user is looking at.
    add(
        CatlyticsMenuItem(
            text = stringResource(
                when (section) {
                    ArtistDetailSection.Songs -> LibraryR.string.library_collection_sort_tracks
                    ArtistDetailSection.Albums -> LibraryR.string.library_collection_sort_albums
                },
            ),
            icon = R.drawable.ic_filter,
            onClick = { onSort(section) },
        ),
    )
    add(CatlyticsMenuDivider)
    if (canRename) {
        add(
            CatlyticsMenuItem(
                text = stringResource(LibraryR.string.library_artist_rename_open),
                icon = R.drawable.ic_edit,
                onClick = onShowRename,
            ),
        )
        add(
            CatlyticsMenuItem(
                text = stringResource(LibraryR.string.library_artist_image_change),
                icon = R.drawable.ic_image,
                onClick = onChangeArtwork,
            ),
        )
        if (content.summary.hasCustomArtwork) {
            add(
                CatlyticsMenuItem(
                    text = stringResource(LibraryR.string.library_artist_image_restore),
                    icon = R.drawable.ic_close,
                    onClick = onRestoreArtwork,
                ),
            )
        }
    }
    add(
        CatlyticsMenuItem(
            text = stringResource(LibraryR.string.library_artist_merge_open),
            icon = R.drawable.ic_artist,
            onClick = onShowMergePicker,
        ),
    )
    if (aliasCount > 0) {
        add(
            CatlyticsMenuItem(
                text = stringResource(LibraryR.string.library_artist_merge_manage, aliasCount),
                icon = R.drawable.ic_list,
                onClick = onShowAliasManager,
            ),
        )
    }
}
