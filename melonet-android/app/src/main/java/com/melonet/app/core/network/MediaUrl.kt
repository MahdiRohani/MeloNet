package com.melonet.app.core.network

import com.melonet.app.BuildConfig

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
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        return if (raw.startsWith('/')) "$base$raw" else "$base/$raw"
    }
}
