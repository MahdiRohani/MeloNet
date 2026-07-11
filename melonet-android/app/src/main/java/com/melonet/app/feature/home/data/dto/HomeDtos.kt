package com.melonet.app.feature.home.data.dto

import com.google.gson.annotations.SerializedName

data class SongDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String?,
    @SerializedName("artist_name") val artistName: String?,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("cover_image_url") val coverImageUrl: String?,
    @SerializedName("audio_url") val audioUrl: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("album_title") val albumTitle: String?,
    @SerializedName("lyrics") val lyrics: String?,
    @SerializedName("duration_sec") val durationSec: Int?
)

data class QuickActionDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("target") val target: String,
    @SerializedName("icon") val icon: String?
)

data class HomeRowDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("row_type") val rowType: String,
    @SerializedName("see_all_path") val seeAllPath: String?,
    @SerializedName("items") val items: List<SongDto>
)

data class HomeFeedDto(
    @SerializedName("carousel") val carousel: List<SongDto>,
    @SerializedName("quick_actions") val quickActions: List<QuickActionDto>,
    @SerializedName("rows") val rows: List<HomeRowDto>
)
