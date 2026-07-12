package com.melonet.app.data.model

data class Playlist(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val title: String,
    val description: String,
    val visibility: String,
    val coverUrl: String,
    val isSystem: Boolean,
    val isOwner: Boolean,
    val songCount: Int,
)

enum class PlaylistScope(val apiValue: String) {
    MINE("mine"),
    SYSTEM("system"),
    FRIENDS("friends"),
    ALL("all"),
}
