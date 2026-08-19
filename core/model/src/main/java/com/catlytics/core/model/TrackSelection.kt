package com.catlytics.core.model

data class TrackSelectionSnapshot(
    val active: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
) {
    val selectedCount: Int get() = selectedIds.size

    fun enter(trackId: String) = copy(active = true, selectedIds = setOf(trackId))

    fun toggle(trackId: String) = copy(
        selectedIds = if (trackId in selectedIds) selectedIds - trackId else selectedIds + trackId,
    )

    fun selectVisible(ids: Collection<String>) = copy(selectedIds = selectedIds + ids)

    fun clear() = TrackSelectionSnapshot()

    fun selectedTracks(from: List<Track>): List<Track> = from.filter { it.id in selectedIds }

    fun allSelectedAreLiked(likedIds: Set<String>): Boolean =
        selectedIds.isNotEmpty() && selectedIds.all { it in likedIds }

    fun onTrackLongClick(trackId: String) = if (active) toggle(trackId) else enter(trackId)
}

sealed interface TrackSelectionAction {
    val tracks: List<Track>

    data class AddToPlaylist(override val tracks: List<Track>) : TrackSelectionAction
    data class Like(override val tracks: List<Track>) : TrackSelectionAction
    data class Unlike(override val tracks: List<Track>) : TrackSelectionAction
    data class AddToQueue(override val tracks: List<Track>) : TrackSelectionAction
    data class PlayNext(override val tracks: List<Track>) : TrackSelectionAction
}
