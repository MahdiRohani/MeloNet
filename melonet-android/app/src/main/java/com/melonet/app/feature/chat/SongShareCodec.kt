package com.melonet.app.feature.chat

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * JSON payload stored in song-share [content] when the track is local / uploaded.
 */
object SongShareCodec {
    private val gson = Gson()

    data class Attachment(
        @SerializedName("audio_url") val audioUrl: String,
        @SerializedName("title") val title: String = "",
        @SerializedName("artist") val artist: String = "",
        @SerializedName("cover") val cover: String = "",
    )

    fun encode(
        audioUrl: String,
        title: String,
        artist: String,
        cover: String,
    ): String = gson.toJson(
        Attachment(
            audioUrl = audioUrl,
            title = title,
            artist = artist,
            cover = cover,
        ),
    )

    fun parse(content: String): Attachment? {
        if (content.isBlank() || !content.trimStart().startsWith("{")) return null
        return runCatching {
            gson.fromJson(content, Attachment::class.java)
                ?.takeIf { it.audioUrl.isNotBlank() }
        }.getOrNull()
    }
}
