package com.melonet.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SearchHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MeloNetDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
}
