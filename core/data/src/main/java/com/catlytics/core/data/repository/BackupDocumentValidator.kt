package com.catlytics.core.data.repository

import com.catlytics.core.model.BackupException
import com.catlytics.core.model.artistIdentityKey

/** Checks a decoded backup before anything is restored, whichever repository read it. */
internal object BackupDocumentValidator {
    const val UNIFIED_FORMAT = "catlytics.backup"
    const val STATISTICS_FORMAT = "catlytics.statistics.backup"
    const val PLAYLISTS_FORMAT = "catlytics.playlists.backup"
    const val SUPPORTED_SCHEMA_VERSION = 4
    const val MIN_SUPPORTED_SCHEMA_VERSION = 1
    const val MAX_BACKUP_BYTES = 64L * 1024L * 1024L

    private val KNOWN_FORMATS = setOf(UNIFIED_FORMAT, STATISTICS_FORMAT, PLAYLISTS_FORMAT)

    fun validate(document: StatisticsBackupDocument) {
        validateHeader(document.format, document.schemaVersion, document.exportedAtMillis)
        validateEvents(document.events)
        validateArtistAliases(document.artistAliases)
        validateMetadataEdits(document.trackMetadataOverrides)
        validatePlaylists(document.playlists)
    }

    fun validate(document: PlaylistBackupDocument) {
        validateHeader(document.format, document.schemaVersion, document.exportedAtMillis)
        validatePlaylists(document.playlists)
    }

    private fun validateHeader(format: String, schemaVersion: Int, exportedAtMillis: Long) {
        if (format !in KNOWN_FORMATS) {
            throw BackupException.InvalidFormat("Unknown backup format '$format'.")
        }
        if (schemaVersion !in MIN_SUPPORTED_SCHEMA_VERSION..SUPPORTED_SCHEMA_VERSION) {
            throw BackupException.UnsupportedVersion(schemaVersion)
        }
        ensure(exportedAtMillis > 0L) { "Invalid export timestamp." }
    }

    private fun validateEvents(events: List<PlaybackEventDto>) {
        events.forEachIndexed { index, event ->
            ensure(event.trackId.isNotBlank()) { "Event $index: blank trackId." }
            ensure(event.trackTitle.isNotBlank()) { "Event $index: blank title." }
            ensure(event.artistId.isNotBlank()) { "Event $index: blank artistId." }
            ensure(event.artistName.isNotBlank()) { "Event $index: blank artist." }
            ensure(event.timestamp > 0L) { "Event $index: invalid timestamp." }
            ensure(event.durationListenedMillis > 0L) { "Event $index: invalid listened duration." }
            ensure(event.trackDurationMillis >= 0L) { "Event $index: invalid track duration." }
        }
    }

    private fun validateArtistAliases(aliases: List<ArtistAliasDto>) {
        aliases.forEachIndexed { index, alias ->
            ensure(alias.sourceArtistId.isNotBlank()) { "Alias $index: blank source id." }
            ensure(alias.sourceArtistName.isNotBlank()) { "Alias $index: blank source." }
            ensure(alias.targetArtistId.isNotBlank()) { "Alias $index: blank target id." }
            ensure(alias.targetArtistName.isNotBlank()) { "Alias $index: blank target." }
            ensure(artistIdentityKey(alias.sourceArtistName) != artistIdentityKey(alias.targetArtistName)) {
                "Alias $index: source and target are the same artist."
            }
        }
        val distinctSources = aliases.map { artistIdentityKey(it.sourceArtistName) }.distinct()
        ensure(distinctSources.size == aliases.size) { "Duplicate artist aliases." }
    }

    private fun validateMetadataEdits(edits: List<TrackMetadataOverrideDto>) {
        edits.forEachIndexed { index, edit ->
            ensure(edit.trackId.isNotBlank()) { "Edit $index: blank trackId." }
            ensure(edit.originalTitle.isNotBlank()) { "Edit $index: blank original title." }
            ensure(edit.hasEdits) { "Edit $index: contains no changes." }
        }
    }

    private fun validatePlaylists(playlists: List<PlaylistBackupDto>) {
        playlists.forEachIndexed { index, playlist ->
            ensure(playlist.id.isNotBlank()) { "Playlist $index: blank id." }
            ensure(playlist.name.isNotBlank()) { "Playlist $index: blank name." }
        }
    }

    private inline fun ensure(condition: Boolean, detail: () -> String) {
        if (!condition) throw BackupException.InvalidContent(detail())
    }
}
