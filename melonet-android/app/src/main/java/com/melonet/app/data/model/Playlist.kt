package com.melonet.app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Playlist(
    val id: Int,
    val ownerId: Int,
    val ownerName: String,
    val title: String,
    val description: String,
    val visibility: String,
    val coverUrl: String,
    val coverUrls: List<String> = emptyList(),
    val isSystem: Boolean,
    val isOwner: Boolean,
    val songCount: Int,
) {
    val displayCoverUrls: List<String>
        get() = coverUrls.filter { it.isNotBlank() }.ifEmpty {
            listOfNotNull(coverUrl.takeIf { it.isNotBlank() })
        }
}

enum class PlaylistScope(val apiValue: String) {
    MINE("mine"),
    SYSTEM("system"),
    FRIENDS("friends"),
    ALL("all"),
}
