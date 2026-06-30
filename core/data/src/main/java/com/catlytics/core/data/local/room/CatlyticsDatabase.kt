package com.catlytics.core.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaybackEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CatlyticsDatabase : RoomDatabase() {
    abstract fun playbackEventDao(): PlaybackEventDao
}
