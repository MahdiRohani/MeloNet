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
        if (raw.startsWith("content://", ignoreCase = true) ||
            raw.startsWith("file://", ignoreCase = true)
        ) {
            return raw
        }
        if (raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true)
        ) {
            // Backend may emit absolute /api/... URLs with a different host than the app
            // (LAN IP vs 127.0.0.1 + adb reverse). Rewrite those to BuildConfig.API_BASE_URL.
            return rewriteApiHostIfNeeded(raw)
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

    /**
     * If [url] points at our API surface (`/api/...`) but uses a different host than
     * [BuildConfig.API_BASE_URL], rewrite scheme+authority to the configured base.
     * Leaves third-party CDN redirects (non-/api paths) untouched.
     */
    private fun rewriteApiHostIfNeeded(url: String): String {
        val parsed = Uri.parse(url)
        val path = parsed.path.orEmpty()
        if (!path.startsWith("/api/")) return url
        val base = Uri.parse(BuildConfig.API_BASE_URL.trimEnd('/'))
        if (base.host.isNullOrBlank()) return url
        if (parsed.host.equals(base.host, ignoreCase = true) &&
            (parsed.port == base.port || (parsed.port == -1 && base.port == -1))
        ) {
            return url
        }
        val rebuilt = parsed.buildUpon()
            .scheme(base.scheme ?: parsed.scheme)
            .encodedAuthority(base.authority)
            .build()
        return rebuilt.toString()
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
