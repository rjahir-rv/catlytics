package com.catlytics.core.domain.repository

/** Exports a playlist to a user-chosen document (SAF Uri as string). */
interface PlaylistExportRepository {
    suspend fun exportPlaylistToM3uUri(playlistId: String, uri: String): Result<Unit>
}
