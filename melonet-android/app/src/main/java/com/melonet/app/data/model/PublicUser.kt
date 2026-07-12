package com.melonet.app.data.model

data class PublicUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val isPremium: Boolean,
    val followerCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean,
    val isSelf: Boolean,
)

data class SocialUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val isPremium: Boolean,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class UserListType {
    FOLLOWERS,
    FOLLOWING,
}
