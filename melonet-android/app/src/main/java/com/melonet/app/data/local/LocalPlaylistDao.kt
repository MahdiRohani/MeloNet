package com.melonet.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalPlaylistDao {
    @Query("SELECT * FROM local_playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    suspend fun getSongsForPlaylist(playlistId: Int): List<LocalPlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: LocalPlaylistSongEntity): Long

    @Query("DELETE FROM local_playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun delete(playlistId: Int, songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM local_playlist_songs WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun exists(playlistId: Int, songId: String): Boolean
}
