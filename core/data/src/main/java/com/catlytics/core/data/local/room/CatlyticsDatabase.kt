package com.catlytics.core.data.local.room

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PlaybackEventEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CatlyticsDatabase : RoomDatabase() {
    abstract fun playbackEventDao(): PlaybackEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playback_events ADD COLUMN album_id TEXT")
                db.execSQL("ALTER TABLE playback_events ADD COLUMN album_title TEXT")
            }
        }
    }
}
