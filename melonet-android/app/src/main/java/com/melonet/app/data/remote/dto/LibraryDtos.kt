package com.melonet.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PlayEventRequestDto(
    @SerializedName("duration_played_sec") val durationPlayedSec: Int,
    @SerializedName("source") val source: String,
)

data class PlayEventResponseDto(
    @SerializedName("song_id") val songId: Int,
    @SerializedName("play_count") val playCount: Int,
)
