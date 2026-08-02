package com.melonet.app.feature.player

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
 * Always prefers a FileProvider cache copy so target apps receive a stable grantable URI
 * (MediaStore content:// URIs often fail on the first share attempt).
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

        val cached = when {
            resolved.startsWith("content://", ignoreCase = true) ||
                resolved.startsWith("file://", ignoreCase = true) -> {
                copyUriToCache(resolved.toUri(), song)
            }
            resolved.startsWith("http://", ignoreCase = true) ||
                resolved.startsWith("https://", ignoreCase = true) -> {
                copyRemoteToCache(resolved, song)
            }
            else -> {
                val file = File(resolved)
                if (!file.exists()) null else copyFileToCache(file, song)
            }
        } ?: return@withContext null

        fileProviderPayload(cached, text, guessMime(cached.name, song))
    }

    fun launchShareChooser(context: Context, payload: AudioSharePayload, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = payload.mimeType
            putExtra(Intent.EXTRA_STREAM, payload.uri)
            putExtra(Intent.EXTRA_TEXT, payload.text)
            clipData = android.content.ClipData.newUri(
                context.contentResolver,
                payload.text,
                payload.uri,
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Explicitly grant every candidate so the first share attempt works.
        val targets = context.packageManager.queryIntentActivities(
            send,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        for (info in targets) {
            context.grantUriPermission(
                info.activityInfo.packageName,
                payload.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        context.startActivity(chooser)
    }

    private fun fileProviderPayload(file: File, text: String, mimeType: String): AudioSharePayload {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return AudioSharePayload(uri = uri, mimeType = mimeType, text = text)
    }

    private fun cacheFileFor(song: Song, ext: String): File {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val safeId = song.id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(48)
        return File(dir, "share_${safeId}$ext")
    }

    private fun copyFileToCache(source: File, song: Song): File? {
        val ext = extensionFor(source.name, song)
        val out = cacheFileFor(song, ext)
        if (out.exists() && out.length() > 0L && out.length() == source.length()) return out
        return runCatching {
            source.inputStream().use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            out
        }.getOrElse {
            out.delete()
            null
        }
    }

    private fun copyUriToCache(uri: Uri, song: Song): File? {
        val ext = extensionFor(uri.toString(), song)
        val out = cacheFileFor(song, ext)
        if (out.exists() && out.length() > 0L) return out
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            } ?: return null
            out
        }.getOrElse {
            out.delete()
            null
        }
    }

    private fun copyRemoteToCache(url: String, song: Song): File? {
        val ext = extensionFor(url, song)
        val out = cacheFileFor(song, ext)
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

    private fun extensionFor(nameOrUrl: String, song: Song): String {
        val path = nameOrUrl.substringBefore('?').lowercase()
        return when {
            path.endsWith(".m4a") || path.contains("audio/mp4") -> ".m4a"
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
            else -> "audio/mpeg"
        }
    }
}
