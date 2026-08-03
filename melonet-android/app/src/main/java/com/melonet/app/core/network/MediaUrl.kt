package com.melonet.app.core.network

import android.net.Uri
import com.melonet.app.BuildConfig
import java.io.File

/**
 * Resolves media/avatar paths that may be absolute or API-relative (`/api/media/...`).
 */
object MediaUrl {
    fun resolve(url: String?): String? {
        val raw = url?.trim().orEmpty()
        if (raw.isEmpty()) return null
        if (raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true) ||
            raw.startsWith("content://", ignoreCase = true) ||
            raw.startsWith("file://", ignoreCase = true)
        ) {
            return raw
        }
        // On-device absolute paths (karaoke takes, downloads) must not be prefixed with API base.
        // Bundled assets use file:///android_asset/... and are already absolute URI form above.
        if (isLocalFilesystemPath(raw)) {
            return Uri.fromFile(File(raw)).toString()
        }
        // Prefer Coil's android asset URI form when callers pass asset-relative paths.
        if (raw.startsWith("asset:///") || raw.startsWith("file:///android_asset/")) {
            return raw
        }
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        return if (raw.startsWith('/')) "$base$raw" else "$base/$raw"
    }

    private fun isLocalFilesystemPath(path: String): Boolean {
        if (!path.startsWith('/')) return false
        if (path.startsWith("/api/")) return false
        return File(path).exists() ||
            path.startsWith("/data/") ||
            path.startsWith("/storage/") ||
            path.startsWith("/sdcard/") ||
            path.startsWith("/mnt/")
    }
}
