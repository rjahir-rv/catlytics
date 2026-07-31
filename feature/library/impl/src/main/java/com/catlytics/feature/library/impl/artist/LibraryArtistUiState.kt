package com.catlytics.feature.library.impl.artist

import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias

internal sealed interface LibraryArtistUiState {
    data object Loading : LibraryArtistUiState
    data object NotFound : LibraryArtistUiState
    data class Success(
        val content: ArtistContent,
        val searchQuery: String = "",
        val mergeCandidates: List<Artist> = emptyList(),
        val aliases: List<ArtistAlias> = emptyList(),
        val mergeDialog: ArtistMergeDialog = ArtistMergeDialog.Hidden,
        val isMergeBusy: Boolean = false,
    ) : LibraryArtistUiState
    data class Error(val message: String) : LibraryArtistUiState
}

internal sealed interface ArtistMergeDialog {
    data object Hidden : ArtistMergeDialog
    data class SelectTarget(val query: String = "") : ArtistMergeDialog
    data class ConfirmMerge(val target: Artist) : ArtistMergeDialog
    data object ManageAliases : ArtistMergeDialog
    data class ConfirmUnmerge(val alias: ArtistAlias) : ArtistMergeDialog
}
