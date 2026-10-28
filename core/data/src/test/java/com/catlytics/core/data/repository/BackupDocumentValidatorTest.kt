package com.catlytics.core.data.repository

import com.catlytics.core.model.BackupException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupDocumentValidatorTest {
    private val event = PlaybackEventDto(
        trackId = "track-1",
        trackTitle = "Clair de Lune",
        artistId = "artist-1",
        artistName = "Claude Debussy",
        durationListenedMillis = 120_000L,
        trackDurationMillis = 300_000L,
        timestamp = 1_760_000_000_000L,
    )
    private val alias = ArtistAliasDto(
        sourceArtistId = "artist-2",
        sourceArtistName = "C. Debussy",
        targetArtistId = "artist-1",
        targetArtistName = "Claude Debussy",
    )
    private val edit = TrackMetadataOverrideDto(
        trackId = "track-1",
        title = "Clair de lune (Suite bergamasque)",
        originalTitle = "Clair de Lune",
        originalArtistName = "Claude Debussy",
    )
    private val artwork = ArtistArtworkDto(artistName = "Claude Debussy", artworkBase64 = "iVBORw0KGgo=")
    private val playlist = PlaylistBackupDto(id = "focus", name = "Focus")

    private val valid = BackupDocument(
        format = BackupCodec.UNIFIED_FORMAT,
        schemaVersion = BackupCodec.SUPPORTED_SCHEMA_VERSION,
        exportedAtMillis = 1_760_000_000_000L,
        events = listOf(event),
        artistAliases = listOf(alias),
        playlists = listOf(playlist),
        trackMetadataOverrides = listOf(edit),
        artistArtworks = listOf(artwork),
    )

    @Test
    fun `a complete document and every legacy format pass`() {
        BackupDocumentValidator.validate(valid)
        BackupDocumentValidator.validate(valid.copy(format = BackupCodec.STATISTICS_FORMAT, schemaVersion = 1))
        BackupDocumentValidator.validate(valid.copy(format = BackupCodec.PLAYLISTS_FORMAT))
        BackupDocumentValidator.validate(
            BackupDocument(BackupCodec.UNIFIED_FORMAT, BackupCodec.SUPPORTED_SCHEMA_VERSION, exportedAtMillis = 1L),
        )
    }

    @Test
    fun `an unknown format is rejected as invalid format`() {
        assertThrows(BackupException.InvalidFormat::class.java) {
            BackupDocumentValidator.validate(valid.copy(format = "spotify.export"))
        }
    }

    @Test
    fun `schema versions outside the supported range are rejected with the version`() {
        listOf(0, BackupCodec.SUPPORTED_SCHEMA_VERSION + 1).forEach { version ->
            val error = assertThrows(BackupException.UnsupportedVersion::class.java) {
                BackupDocumentValidator.validate(valid.copy(schemaVersion = version))
            }
            assertEquals(version, error.schemaVersion)
        }
    }

    @Test
    fun `a missing export timestamp is rejected`() {
        assertInvalid(valid.copy(exportedAtMillis = 0L))
    }

    @Test
    fun `events with missing identity or impossible durations are rejected`() {
        listOf(
            event.copy(trackId = " "),
            event.copy(trackTitle = ""),
            event.copy(artistId = ""),
            event.copy(artistName = "  "),
            event.copy(timestamp = 0L),
            event.copy(durationListenedMillis = 0L),
            event.copy(trackDurationMillis = -1L),
        ).forEach { broken ->
            assertInvalid(valid.copy(events = listOf(event, broken)))
        }
    }

    @Test
    fun `an alias pointing to the same artist is rejected even with other casing`() {
        assertInvalid(valid.copy(artistAliases = listOf(alias.copy(sourceArtistName = "  claude   DEBUSSY"))))
    }

    @Test
    fun `aliases with blank fields or a repeated source are rejected`() {
        listOf(
            listOf(alias.copy(sourceArtistId = "")),
            listOf(alias.copy(sourceArtistName = "")),
            listOf(alias.copy(targetArtistId = " ")),
            listOf(alias.copy(targetArtistName = "")),
            listOf(alias, alias.copy(sourceArtistId = "artist-3", sourceArtistName = "c. debussy")),
        ).forEach { aliases ->
            assertInvalid(valid.copy(artistAliases = aliases))
        }
    }

    @Test
    fun `metadata edits without changes or identity are rejected`() {
        listOf(
            edit.copy(title = null),
            edit.copy(trackId = ""),
            edit.copy(originalTitle = " "),
        ).forEach { broken ->
            assertInvalid(valid.copy(trackMetadataOverrides = listOf(broken)))
        }
    }

    @Test
    fun `artist images need an artist, an image and one entry per artist`() {
        listOf(
            listOf(artwork.copy(artistName = "")),
            listOf(artwork.copy(artworkBase64 = "")),
            listOf(artwork, artwork.copy(artistName = "CLAUDE DEBUSSY")),
        ).forEach { artworks ->
            assertInvalid(valid.copy(artistArtworks = artworks))
        }
    }

    @Test
    fun `playlists need an id and a name`() {
        assertInvalid(valid.copy(playlists = listOf(playlist.copy(id = ""))))
        assertInvalid(valid.copy(playlists = listOf(playlist.copy(name = "   "))))
    }

    private fun assertInvalid(document: BackupDocument) {
        assertThrows(BackupException.InvalidContent::class.java) {
            BackupDocumentValidator.validate(document)
        }
    }
}
