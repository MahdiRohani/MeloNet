package com.melonet.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_playlist_songs")
data class LocalPlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Int,
    val songId: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val audioUrl: String,
    val durationSec: Int,
    val isLocal: Boolean,
    val addedAt: Long,
)
