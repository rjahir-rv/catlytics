package com.catlytics.feature.library.impl.artist

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.Track
import com.catlytics.core.model.UNKNOWN_ARTIST_NAME
import com.catlytics.core.model.artistIdentityKey

internal sealed interface LibraryArtistUiState {
    data object Loading : LibraryArtistUiState
    data object NotFound : LibraryArtistUiState
    data class Success(
        val content: ArtistContent,
        val searchQuery: String = "",
        val playbackQueue: List<Track> = content.tracks,
        val mergeCandidates: List<Artist> = emptyList(),
        val aliases: List<ArtistAlias> = emptyList(),
        val mergeDialog: ArtistMergeDialog = ArtistMergeDialog.Hidden,
        val isMergeBusy: Boolean = false,
    ) : LibraryArtistUiState {
        val canRename: Boolean get() = content.summary.artist.name != UNKNOWN_ARTIST_NAME
        val renameMergeTarget: Artist?
            get() {
                val name = (mergeDialog as? ArtistMergeDialog.Rename)?.name ?: return null
                val key = artistIdentityKey(name)
                return mergeCandidates.firstOrNull { artistIdentityKey(it.name) == key }
            }
    }
    data class Error(val message: UiText) : LibraryArtistUiState
}

internal sealed interface ArtistMergeDialog {
    data object Hidden : ArtistMergeDialog
    data class SelectTarget(val query: String = "") : ArtistMergeDialog
    data class ConfirmMerge(val target: Artist) : ArtistMergeDialog
    data object ManageAliases : ArtistMergeDialog
    data class ConfirmUnmerge(val alias: ArtistAlias) : ArtistMergeDialog
    data class Rename(val name: String) : ArtistMergeDialog
}
