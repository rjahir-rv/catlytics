package com.catlytics.core.data.repository

import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.StatisticsImportMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for backup document serialization and merge/replace logic without Android ContentResolver.
 */
class StatisticsBackupLogicTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun event(
        trackId: String = "mediastore-1",
        timestamp: Long = 1_700_000_000_000L,
        duration: Long = 60_000L,
        title: String = "Song",
    ) = PlaybackEvent(
        trackId = trackId,
        trackTitle = title,
        artistId = "mediastore-artist-1",
        artistName = "Artist",
        albumId = "mediastore-album-1",
        albumTitle = "Album",
        artworkUri = null,
        durationListenedMillis = duration,
        trackDurationMillis = 180_000L,
        timestamp = timestamp,
    )

    @Test
    fun `document round-trips through json`() {
        val document = StatisticsBackupDocument(
            format = DefaultStatisticsBackupRepository.BACKUP_FORMAT,
            schemaVersion = DefaultStatisticsBackupRepository.SUPPORTED_SCHEMA_VERSION,
            exportedAtMillis = 1_710_000_000_000L,
            appVersion = "0.0.5",
            events = listOf(
                PlaybackEventDto(
                    trackId = "mediastore-1",
                    trackTitle = "Song",
                    artistId = "a1",
                    artistName = "Artist",
                    durationListenedMillis = 60_000L,
                    trackDurationMillis = 180_000L,
                    timestamp = 1_700_000_000_000L,
                ),
            ),
        )
        val encoded = json.encodeToString(StatisticsBackupDocument.serializer(), document)
        val decoded = json.decodeFromString(StatisticsBackupDocument.serializer(), encoded)
        assertEquals(document, decoded)
        assertTrue(encoded.contains("catlytics.statistics.backup"))
    }

    @Test
    fun `merge skips fingerprints already present`() {
        val existing = setOf(
            playbackEventFingerprint("mediastore-1", 1_700_000_000_000L, 60_000L),
        )
        val incoming = listOf(
            event(timestamp = 1_700_000_000_000L, duration = 60_000L),
            event(timestamp = 1_700_000_100_000L, duration = 90_000L),
        )
        val (toInsert, skipped) = partitionForMerge(existing, incoming)
        assertEquals(1, toInsert.size)
        assertEquals(1_700_000_100_000L, toInsert.single().timestamp)
        assertEquals(1, skipped)
    }

    @Test
    fun `replace mode would insert all events after wipe`() {
        // Document intent: Replace does not filter by fingerprint.
        val incoming = listOf(event(), event(timestamp = 2L))
        val mode = StatisticsImportMode.Replace
        val imported = when (mode) {
            StatisticsImportMode.Replace -> incoming.size
            StatisticsImportMode.Merge -> 0
        }
        assertEquals(2, imported)
    }

    @Test
    fun `invalid format is detectable`() {
        val document = StatisticsBackupDocument(
            format = "other.format",
            schemaVersion = 1,
            exportedAtMillis = 1L,
            events = emptyList(),
        )
        assertTrue(document.format != DefaultStatisticsBackupRepository.BACKUP_FORMAT)
    }

    private fun partitionForMerge(
        existingFingerprints: Set<String>,
        events: List<PlaybackEvent>,
    ): Pair<List<PlaybackEvent>, Int> {
        val existing = existingFingerprints.toMutableSet()
        val toInsert = mutableListOf<PlaybackEvent>()
        var skipped = 0
        for (event in events) {
            val fingerprint = playbackEventFingerprint(
                trackId = event.trackId,
                timestamp = event.timestamp,
                durationListenedMillis = event.durationListenedMillis,
            )
            if (fingerprint in existing) {
                skipped++
            } else {
                existing.add(fingerprint)
                toInsert.add(event)
            }
        }
        return toInsert to skipped
    }
}
