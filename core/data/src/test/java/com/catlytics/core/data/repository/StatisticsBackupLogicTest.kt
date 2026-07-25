package com.catlytics.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.StatisticsImportMode
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StatisticsBackupLogicTest {

    private lateinit var context: Context
    private lateinit var database: CatlyticsDatabase
    private lateinit var eventRepository: RoomPlaybackEventRepository
    private lateinit var backupRepository: DefaultStatisticsBackupRepository
    private val temporaryFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, CatlyticsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepository = RoomPlaybackEventRepository(database.playbackEventDao())
        backupRepository = DefaultStatisticsBackupRepository(context, eventRepository)
    }

    @After
    fun tearDown() {
        database.close()
        temporaryFiles.forEach(File::delete)
    }

    @Test
    fun `export and replace import round-trip all statistics fields`() = runTest {
        val original = listOf(
            event(),
            event(
                trackId = "mediastore-2",
                timestamp = 1_700_000_100_000L,
                title = "Otra canción",
            ),
        )
        original.forEach { eventRepository.recordEvent(it) }
        val backupUri = newBackupUri()

        val exportResult = backupRepository.exportToUri(backupUri, "0.0.5").getOrThrow()
        assertEquals(original.size, exportResult.eventCount)

        val preview = backupRepository.previewFromUri(backupUri).getOrThrow()
        assertEquals(original.size, preview.eventCount)
        assertEquals(original.first().timestamp, preview.firstEventMillis)
        assertEquals(original.last().timestamp, preview.lastEventMillis)

        eventRepository.replaceEvents(listOf(event(trackId = "temporary", timestamp = 2L)))
        val importResult = backupRepository.importFromUri(
            backupUri,
            StatisticsImportMode.Replace,
        ).getOrThrow()

        assertEquals(original.size, importResult.importedCount)
        assertEquals(original, eventRepository.getAllEvents())
    }

    @Test
    fun `merge is idempotent and reports skipped duplicates`() = runTest {
        val original = listOf(
            event(),
            event(trackId = "mediastore-2", timestamp = 1_700_000_100_000L),
        )
        original.forEach { eventRepository.recordEvent(it) }
        val backupUri = newBackupUri()
        backupRepository.exportToUri(backupUri, "0.0.5").getOrThrow()
        eventRepository.replaceEvents(emptyList())

        val firstImport = backupRepository.importFromUri(
            backupUri,
            StatisticsImportMode.Merge,
        ).getOrThrow()
        val secondImport = backupRepository.importFromUri(
            backupUri,
            StatisticsImportMode.Merge,
        ).getOrThrow()

        assertEquals(2, firstImport.importedCount)
        assertEquals(0, firstImport.skippedDuplicateCount)
        assertEquals(0, secondImport.importedCount)
        assertEquals(2, secondImport.skippedDuplicateCount)
        assertEquals(original, eventRepository.getAllEvents())
    }

    @Test
    fun `merge skips duplicate fingerprints within backup file`() = runTest {
        val first = event(title = "Título original")
        val duplicateFingerprint = first.copy(trackTitle = "Título modificado")
        eventRepository.recordEvent(first)
        eventRepository.recordEvent(duplicateFingerprint)
        val backupUri = newBackupUri()
        backupRepository.exportToUri(backupUri, "0.0.5").getOrThrow()
        eventRepository.replaceEvents(emptyList())

        val result = backupRepository.importFromUri(
            backupUri,
            StatisticsImportMode.Merge,
        ).getOrThrow()

        assertEquals(1, result.importedCount)
        assertEquals(1, result.skippedDuplicateCount)
        assertEquals(listOf(first), eventRepository.getAllEvents())
    }

    @Test
    fun `invalid document fails without changing existing events`() = runTest {
        val existing = event()
        eventRepository.recordEvent(existing)
        val invalidFile = newBackupFile().apply {
            writeText("""{"format":"otro.formato","schemaVersion":1}""")
        }

        val result = backupRepository.importFromUri(
            Uri.fromFile(invalidFile).toString(),
            StatisticsImportMode.Replace,
        )

        assertTrue(result.isFailure)
        assertEquals(listOf(existing), eventRepository.getAllEvents())
    }

    @Test
    fun `replace rolls back deletion when an insert fails`() = runTest {
        val existing = event()
        eventRepository.recordEvent(existing)
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_forced_import
            BEFORE INSERT ON playback_events
            WHEN NEW.track_id = 'force-failure'
            BEGIN
                SELECT RAISE(ABORT, 'forced import failure');
            END
            """.trimIndent(),
        )

        try {
            eventRepository.replaceEvents(listOf(event(trackId = "force-failure")))
            fail("La inserción forzada debía fallar")
        } catch (_: Exception) {
            // Expected: Room must roll the deletion back with the failed insert.
        }

        assertEquals(listOf(existing), eventRepository.getAllEvents())
    }

    @Test
    fun `import propagates coroutine cancellation`() = runTest {
        eventRepository.recordEvent(event())
        val backupUri = newBackupUri()
        backupRepository.exportToUri(backupUri, "0.0.5").getOrThrow()
        val cancellingRepository = DefaultStatisticsBackupRepository(
            context,
            object : PlaybackEventRepository by eventRepository {
                override suspend fun replaceEvents(events: List<PlaybackEvent>) {
                    throw CancellationException("test cancellation")
                }
            },
        )

        try {
            cancellingRepository.importFromUri(backupUri, StatisticsImportMode.Replace)
            fail("La cancelación debía propagarse")
        } catch (_: CancellationException) {
            // Expected: cancellation must not be converted into Result.failure.
        }
    }

    private fun newBackupUri(): String = Uri.fromFile(newBackupFile()).toString()

    private fun newBackupFile(): File = File.createTempFile(
        "catlytics-statistics-",
        ".json",
        context.cacheDir,
    ).also(temporaryFiles::add)

    private fun event(
        trackId: String = "mediastore-1",
        timestamp: Long = 1_700_000_000_000L,
        title: String = "Canción",
    ) = PlaybackEvent(
        trackId = trackId,
        trackTitle = title,
        artistId = "mediastore-artist-1",
        artistName = "Artista",
        albumId = "mediastore-album-1",
        albumTitle = "Álbum",
        artworkUri = "content://media/external/audio/albumart/1",
        durationListenedMillis = 60_000L,
        trackDurationMillis = 180_000L,
        timestamp = timestamp,
    )
}
