package com.melonet.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedSongDao {
    @Query("SELECT * FROM liked_songs ORDER BY cachedAt DESC")
    fun observeAll(): Flow<List<LikedSongEntity>>

    @Query("SELECT * FROM liked_songs ORDER BY cachedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(offset: Int, limit: Int): List<LikedSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<LikedSongEntity>)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM liked_songs")
    suspend fun clear()
}

@Dao
interface PlayHistoryDao {
    @Query("SELECT * FROM play_history ORDER BY playedAt DESC")
    fun observeAll(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(offset: Int, limit: Int): List<PlayHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<PlayHistoryEntity>)

    @Query("DELETE FROM play_history WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM play_history")
    suspend fun clear()
}
