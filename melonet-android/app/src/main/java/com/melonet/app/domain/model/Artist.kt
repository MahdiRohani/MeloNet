package com.melonet.app.domain.model

data class Artist(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val bio: String?
)

data class Playlist(
    val id: Int,
    val title: String,
    val coverUrl: String,
    val songCount: Int,
    val isOwner: Boolean
)
