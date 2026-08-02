package com.melonet.app.feature.player

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.melonet.app.data.model.Song
import com.melonet.app.data.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class AudioSharePayload(
    val uri: Uri,
    val mimeType: String,
    val text: String,
)

/**
 * Prepares a shareable audio URI for ACTION_SEND + EXTRA_STREAM.
 * Local/media URIs are granted as-is; downloads use FileProvider;
 * remote streams are copied into cache first.
 */
class AudioShareHelper(
    private val context: Context,
    private val downloadRepository: DownloadRepository,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun prepare(song: Song): AudioSharePayload? = withContext(Dispatchers.IO) {
        val text = "${song.title} — ${song.artistName}"
        val resolved = downloadRepository.localPathFor(song.id)
            ?.takeIf { File(it).exists() }
            ?: song.audioUrl.takeIf { it.isNotBlank() }
            ?: return@withContext null

        when {
            resolved.startsWith("content://", ignoreCase = true) -> {
                AudioSharePayload(
                    uri = resolved.toUri(),
                    mimeType = guessMime(resolved, song),
                    text = text,
                )
            }
            resolved.startsWith("file://", ignoreCase = true) -> {
                val file = File(resolved.removePrefix("file://"))
                if (!file.exists()) return@withContext null
                fileProviderPayload(file, text, guessMime(file.name, song))
            }
            !resolved.startsWith("http://", ignoreCase = true) &&
                !resolved.startsWith("https://", ignoreCase = true) -> {
                val file = File(resolved)
                if (!file.exists()) return@withContext null
                fileProviderPayload(file, text, guessMime(file.name, song))
            }
            else -> {
                val cached = copyRemoteToCache(resolved, song) ?: return@withContext null
                fileProviderPayload(cached, text, guessMime(cached.name, song))
            }
        }
    }

    private fun fileProviderPayload(file: File, text: String, mimeType: String): AudioSharePayload {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return AudioSharePayload(uri = uri, mimeType = mimeType, text = text)
    }

    private fun copyRemoteToCache(url: String, song: Song): File? {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val ext = extensionFor(url, song)
        val safeId = song.id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(48)
        val out = File(dir, "share_${safeId}$ext")
        if (out.exists() && out.length() > 0L) return out

        return runCatching {
            val response = okHttpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }
            response.use { resp ->
                val body = resp.body ?: return null
                body.byteStream().use { input ->
                    FileOutputStream(out).use { output -> input.copyTo(output) }
                }
            }
            out
        }.getOrElse {
            out.delete()
            null
        }
    }

    private fun extensionFor(url: String, song: Song): String {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".m4a") -> ".m4a"
            path.endsWith(".wav") -> ".wav"
            path.endsWith(".ogg") -> ".ogg"
            path.endsWith(".flac") -> ".flac"
            path.endsWith(".aac") -> ".aac"
            song.category == "karaoke" -> ".m4a"
            else -> ".mp3"
        }
    }

    private fun guessMime(nameOrUrl: String, song: Song): String {
        val lower = nameOrUrl.lowercase()
        return when {
            lower.endsWith(".m4a") || lower.endsWith(".mp4") || song.category == "karaoke" -> "audio/mp4"
            lower.endsWith(".wav") -> "audio/wav"
            lower.endsWith(".ogg") -> "audio/ogg"
            lower.endsWith(".flac") -> "audio/flac"
            lower.endsWith(".aac") -> "audio/aac"
            else -> "audio/*"
        }
    }
}
