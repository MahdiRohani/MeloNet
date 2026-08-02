package com.melonet.app.data.repository

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.data.local.KaraokeRecordingDao
import com.melonet.app.data.local.KaraokeRecordingEntity
import com.melonet.app.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

data class KaraokeRecording(
    val id: Long,
    val songId: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val instrumentalUrl: String,
    val vocalPath: String,
    val durationSec: Int,
    val createdAt: Long,
) {
    fun asSong(): Song = Song(
        id = "karaoke_$id",
        title = "$title (My take)",
        artistName = artistName,
        coverUrl = coverUrl,
        audioUrl = vocalPath,
        category = "karaoke",
        lyrics = "",
        durationSec = durationSec,
    )
}

class KaraokeRecordingRepository(
    private val context: Context,
    private val dao: KaraokeRecordingDao,
    private val dispatchers: DispatchersProvider,
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun observeRecordings(): Flow<List<KaraokeRecording>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getById(id: Long): KaraokeRecording? = withContext(dispatchers.io) {
        dao.getById(id)?.toModel()
    }

    fun startRecording(): File {
        stopRecordingInternal(delete = true)
        val dir = File(context.filesDir, "karaoke_recordings").apply { mkdirs() }
        val file = File(dir, "take_${System.currentTimeMillis()}.m4a")

        var lastError: Throwable? = null
        for (source in micSources()) {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            val ok = runCatching {
                mediaRecorder.setAudioSource(source)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setAudioEncodingBitRate(128_000)
                mediaRecorder.setAudioSamplingRate(44_100)
                mediaRecorder.setOutputFile(file.absolutePath)
                mediaRecorder.prepare()
                mediaRecorder.start()
            }
            if (ok.isSuccess) {
                recorder = mediaRecorder
                currentFile = file
                return file
            }
            lastError = ok.exceptionOrNull()
            runCatching { mediaRecorder.release() }
        }
        file.delete()
        throw lastError ?: IllegalStateException("Could not start MediaRecorder")
    }

    fun stopRecording(): File? {
        return stopRecordingInternal(delete = false)
    }

    fun cancelRecording() {
        stopRecordingInternal(delete = true)
    }

    suspend fun saveRecording(
        song: Song,
        vocalFile: File,
        durationSec: Int,
    ): KaraokeRecording = withContext(dispatchers.io) {
        val id = dao.insert(
            KaraokeRecordingEntity(
                songId = song.id,
                title = song.title,
                artistName = song.artistName,
                coverUrl = song.coverUrl,
                instrumentalUrl = song.audioUrl,
                vocalPath = vocalFile.absolutePath,
                durationSec = durationSec.coerceAtLeast(1),
            ),
        )
        KaraokeRecording(
            id = id,
            songId = song.id,
            title = song.title,
            artistName = song.artistName,
            coverUrl = song.coverUrl,
            instrumentalUrl = song.audioUrl,
            vocalPath = vocalFile.absolutePath,
            durationSec = durationSec.coerceAtLeast(1),
            createdAt = System.currentTimeMillis(),
        )
    }

    suspend fun delete(id: Long) = withContext(dispatchers.io) {
        val existing = dao.getById(id)
        if (existing != null) {
            File(existing.vocalPath).delete()
            dao.delete(id)
        }
    }

    private fun micSources(): List<Int> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            add(MediaRecorder.AudioSource.UNPROCESSED)
        }
        add(MediaRecorder.AudioSource.CAMCORDER)
        add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        add(MediaRecorder.AudioSource.MIC)
    }

    private fun stopRecordingInternal(delete: Boolean): File? {
        val file = currentFile
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // stop can throw if nothing was recorded
        }
        recorder = null
        currentFile = null
        if (delete) {
            file?.delete()
            return null
        }
        return file
    }

    private fun KaraokeRecordingEntity.toModel() = KaraokeRecording(
        id = id,
        songId = songId,
        title = title,
        artistName = artistName,
        coverUrl = coverUrl,
        instrumentalUrl = instrumentalUrl,
        vocalPath = vocalPath,
        durationSec = durationSec,
        createdAt = createdAt,
    )
}
