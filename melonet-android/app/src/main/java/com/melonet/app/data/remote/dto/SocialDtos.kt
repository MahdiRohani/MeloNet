package com.melonet.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PublicUserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("follower_count") val followerCount: Int,
    @SerializedName("following_count") val followingCount: Int,
    @SerializedName("is_following") val isFollowing: Boolean,
    @SerializedName("is_self") val isSelf: Boolean,
)

data class FollowResponseDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("following") val following: Boolean,
    @SerializedName("follower_count") val followerCount: Int,
)

data class SocialUserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("is_premium") val isPremium: Boolean,
)
