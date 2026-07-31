package com.catlytics.core.data.local.room

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlaybackEventEntity::class, ArtistAliasEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CatlyticsDatabase : RoomDatabase() {
    abstract fun playbackEventDao(): PlaybackEventDao
    abstract fun artistAliasDao(): ArtistAliasDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playback_events ADD COLUMN album_id TEXT")
                db.execSQL("ALTER TABLE playback_events ADD COLUMN album_title TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE playback_events ADD COLUMN artist_key TEXT NOT NULL DEFAULT ''",
                )
                db.query("SELECT id, artist_name FROM playback_events").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    val nameIndex = cursor.getColumnIndexOrThrow("artist_name")
                    val statement = db.compileStatement(
                        "UPDATE playback_events SET artist_key = ? WHERE id = ?",
                    )
                    while (cursor.moveToNext()) {
                        statement.bindString(
                            1,
                            com.catlytics.core.model.artistIdentityKey(cursor.getString(nameIndex)),
                        )
                        statement.bindLong(2, cursor.getLong(idIndex))
                        statement.executeUpdateDelete()
                        statement.clearBindings()
                    }
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS artist_aliases (
                        source_key TEXT NOT NULL PRIMARY KEY,
                        source_artist_id TEXT NOT NULL,
                        source_artist_name TEXT NOT NULL,
                        target_key TEXT NOT NULL,
                        target_artist_id TEXT NOT NULL,
                        target_artist_name TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_artist_aliases_target_key " +
                        "ON artist_aliases(target_key)",
                )
            }
        }
    }
}
