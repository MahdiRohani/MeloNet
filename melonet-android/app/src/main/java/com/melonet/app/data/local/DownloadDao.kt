package com.melonet.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE songId = :songId LIMIT 1")
    fun observeBySongId(songId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE songId = :songId LIMIT 1")
    suspend fun getBySongId(songId: String): DownloadEntity?

    @Query("SELECT filePath FROM downloads WHERE songId = :songId AND status = 'COMPLETED' LIMIT 1")
    suspend fun getCompletedFilePath(songId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query(
        """
        UPDATE downloads
        SET status = :status, progress = :progress, filePath = :filePath, downloadedAt = :downloadedAt
        WHERE songId = :songId
        """,
    )
    suspend fun updateProgress(
        songId: String,
        status: String,
        progress: Int,
        filePath: String,
        downloadedAt: Long,
    )

    @Query("DELETE FROM downloads WHERE songId = :songId")
    suspend fun delete(songId: String)
}
