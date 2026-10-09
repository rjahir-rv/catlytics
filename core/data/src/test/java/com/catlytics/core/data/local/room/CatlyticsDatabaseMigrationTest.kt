package com.catlytics.core.data.local.room

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CatlyticsDatabaseMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun `migration 3 to 4 keeps events and adds overrides with the resolved view`() = runTest {
        createVersion3Database()

        val database = Room.databaseBuilder(context, CatlyticsDatabase::class.java, DB_NAME)
            .addMigrations(
                CatlyticsDatabase.MIGRATION_1_2,
                CatlyticsDatabase.MIGRATION_2_3,
                CatlyticsDatabase.MIGRATION_3_4,
            )
            .allowMainThreadQueries()
            .build()
        try {
            database.trackMetadataOverrideDao().upsert(
                TrackMetadataOverrideEntity(
                    trackId = "track-1",
                    fileKey = "external:Music/a.mp3",
                    title = "Editada",
                    artistName = null,
                    artistKey = null,
                    artistId = null,
                    albumTitle = null,
                    albumId = null,
                    artworkUri = null,
                    originalTitle = "Original",
                    originalArtistName = "Artista",
                    originalAlbumTitle = null,
                    durationMillis = 1_000L,
                    updatedAtMillis = 1L,
                ),
            )

            val topTracks = database.playbackEventDao().observeTopTracks(0L, 10_000L, 10).first()
            assertEquals(listOf("Editada"), topTracks.map { it.title })
        } finally {
            database.close()
        }
    }

    private fun createVersion3Database() {
        val path = context.getDatabasePath(DB_NAME).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL(
                "CREATE TABLE `playback_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`track_id` TEXT NOT NULL, `track_title` TEXT NOT NULL, " +
                    "`artist_id` TEXT NOT NULL, `artist_name` TEXT NOT NULL, " +
                    "`artist_key` TEXT NOT NULL DEFAULT '', `album_id` TEXT, `album_title` TEXT, " +
                    "`artwork_uri` TEXT, `duration_listened_millis` INTEGER NOT NULL, " +
                    "`track_duration_millis` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE INDEX `index_playback_events_timestamp` ON `playback_events` (`timestamp`)")
            db.execSQL(
                "CREATE INDEX `index_playback_events_track_id_timestamp` " +
                    "ON `playback_events` (`track_id`, `timestamp`)",
            )
            db.execSQL(
                "CREATE INDEX `index_playback_events_artist_id_timestamp` " +
                    "ON `playback_events` (`artist_id`, `timestamp`)",
            )
            db.execSQL(
                "CREATE TABLE `artist_aliases` (`source_key` TEXT NOT NULL, " +
                    "`source_artist_id` TEXT NOT NULL, `source_artist_name` TEXT NOT NULL, " +
                    "`target_key` TEXT NOT NULL, `target_artist_id` TEXT NOT NULL, " +
                    "`target_artist_name` TEXT NOT NULL, PRIMARY KEY(`source_key`))",
            )
            db.execSQL("CREATE INDEX `index_artist_aliases_target_key` ON `artist_aliases` (`target_key`)")
            db.execSQL(
                "INSERT INTO playback_events (track_id, track_title, artist_id, artist_name, " +
                    "artist_key, duration_listened_millis, track_duration_millis, timestamp) " +
                    "VALUES ('track-1', 'Original', 'artist', 'Artista', 'artista', 500, 1000, 1000)",
            )
            db.version = 3
        }
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
