package com.melonet.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val audioUrl: String,
    val durationSec: Int,
    val cachedAt: Long,
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val audioUrl: String,
    val durationSec: Int,
    val playedAt: Long,
)
