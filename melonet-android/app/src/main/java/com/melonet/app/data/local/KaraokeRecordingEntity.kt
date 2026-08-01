package com.melonet.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "karaoke_recordings")
data class KaraokeRecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val instrumentalUrl: String,
    val vocalPath: String,
    val durationSec: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
