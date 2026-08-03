package com.melonet.app.feature.voicecover

import android.content.Context
import java.io.IOException

/** Local bundled portraits under assets/voice_artists/{slug}.jpg */
object VoiceArtistAvatars {
    fun resolve(context: Context, slug: String, remoteUrl: String): String? {
        val asset = "voice_artists/$slug.jpg"
        if (assetExists(context, asset)) {
            return "file:///android_asset/$asset"
        }
        return remoteUrl.trim().takeIf { it.isNotEmpty() }
    }

    private fun assetExists(context: Context, path: String): Boolean =
        try {
            context.assets.open(path).close()
            true
        } catch (_: IOException) {
            false
        }
}
