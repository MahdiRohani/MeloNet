package com.melonet.app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class VoiceArtist(
    val id: Long,
    val slug: String,
    val displayName: String,
    val avatarUrl: String,
    val enabled: Boolean,
)

@Immutable
data class VoiceCover(
    val id: Long,
    val sourceSongId: String,
    val targetArtistId: Long,
    val targetArtistSlug: String,
    val targetArtistName: String,
    val status: String,
    val audioUrl: String,
    val coverUrl: String,
    val sourceTitle: String,
    val sourceArtist: String,
    val error: String,
    val progressPct: Int = 0,
    val progressStage: String = "",
    val etaSeconds: Int = 0,
) {
    val isReady: Boolean get() = status == "ready" && audioUrl.isNotBlank()
    val isFailed: Boolean get() = status == "failed"
    val isInProgress: Boolean get() = status == "pending" || status == "processing"

    fun toSong(): Song = Song(
        id = "vc-$id",
        title = sourceTitle.ifBlank { "Voice Cover #$id" },
        artistName = targetArtistName.ifBlank { sourceArtist },
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        category = "voice_cover",
        lyrics = "",
        durationSec = 0,
        genre = targetArtistName,
    )
}
