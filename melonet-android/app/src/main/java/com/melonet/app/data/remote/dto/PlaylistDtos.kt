package com.melonet.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PlaylistDto(
    @SerializedName("id") val id: Int,
    @SerializedName("owner_id") val ownerId: Int,
    @SerializedName("owner_name") val ownerName: String?,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("visibility") val visibility: String,
    @SerializedName("cover_url") val coverUrl: String?,
    @SerializedName("is_system") val isSystem: Boolean = false,
    @SerializedName("is_owner") val isOwner: Boolean = false,
    @SerializedName("song_count") val songCount: Int = 0,
)

data class CreatePlaylistRequestDto(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("visibility") val visibility: String = "private",
)

data class AddPlaylistSongRequestDto(
    @SerializedName("song_id") val songId: Int,
)
