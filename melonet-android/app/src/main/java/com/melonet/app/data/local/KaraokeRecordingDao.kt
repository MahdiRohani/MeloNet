package com.melonet.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KaraokeRecordingDao {
    @Query("SELECT * FROM karaoke_recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<KaraokeRecordingEntity>>

    @Query("SELECT * FROM karaoke_recordings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): KaraokeRecordingEntity?

    @Insert
    suspend fun insert(entity: KaraokeRecordingEntity): Long

    @Query("DELETE FROM karaoke_recordings WHERE id = :id")
    suspend fun delete(id: Long)
}
