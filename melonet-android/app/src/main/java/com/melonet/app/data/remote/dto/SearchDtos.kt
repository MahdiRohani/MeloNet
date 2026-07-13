package com.melonet.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SearchResponseDto(
    @SerializedName("query") val query: String,
    @SerializedName("type") val type: String,
    @SerializedName("songs") val songs: List<SongDto>?,
    @SerializedName("artists") val artists: List<ArtistDto>?,
    @SerializedName("users") val users: List<UserSearchResultDto>?,
)

data class ArtistDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("song_count") val songCount: Int?,
    @SerializedName("region") val region: String?,
    @SerializedName("is_following") val isFollowing: Boolean?,
)

data class UserSearchResultDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("is_premium") val isPremium: Boolean?,
)
