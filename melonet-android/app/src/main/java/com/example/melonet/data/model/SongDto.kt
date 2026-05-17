package com.example.melonet.data.model

import com.google.gson.annotations.SerializedName

data class SongDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("cover_url") val coverUrl: String,
    @SerializedName("audio_url") val audioUrl: String,
    @SerializedName("category") val category: String,
    @SerializedName("lyrics") val lyrics: String,
    @SerializedName("duration_sec") val durationSec: Int
)
