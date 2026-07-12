package com.melonet.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SearchHistoryEntity::class,
        LikedSongEntity::class,
        PlayHistoryEntity::class,
        DownloadEntity::class,
        ChatMessageEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class MeloNetDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun likedSongDao(): LikedSongDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun chatMessageDao(): ChatMessageDao
}
