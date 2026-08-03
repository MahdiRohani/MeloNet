package com.melonet.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VoiceArtistDto(
    @SerializedName("id") val id: Long,
    @SerializedName("slug") val slug: String,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("enabled") val enabled: Boolean?,
)

data class VoiceCoverDto(
    @SerializedName("id") val id: Long,
    @SerializedName("source_song_id") val sourceSongId: String?,
    @SerializedName("target_artist_id") val targetArtistId: Long?,
    @SerializedName("target_artist_slug") val targetArtistSlug: String?,
    @SerializedName("target_artist_name") val targetArtistName: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("audio_url") val audioUrl: String?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("source_title") val sourceTitle: String?,
    @SerializedName("source_artist") val sourceArtist: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("progress_pct") val progressPct: Int?,
    @SerializedName("progress_stage") val progressStage: String?,
    @SerializedName("eta_seconds") val etaSeconds: Int?,
)

data class CreateVoiceCoverRequestDto(
    @SerializedName("source_song_id") val sourceSongId: String,
    @SerializedName("target_artist_slug") val targetArtistSlug: String,
)

data class DeletedDto(
    @SerializedName("deleted") val deleted: Boolean? = true,
)
