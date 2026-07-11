package com.melonet.app.domain.model

data class User(
    val id: Int,
    val username: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val bio: String,
    val isPremium: Boolean
)
