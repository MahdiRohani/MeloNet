package com.melonet.app.data.model

enum class SearchFilter(val apiValue: String) {
    ALL("all"),
    SONG("song"),
    ARTIST("artist"),
    USER("user"),
}

sealed interface SearchResultItem {
    data class SongItem(val song: Song) : SearchResultItem
    data class ArtistItem(val artist: Artist) : SearchResultItem
    data class UserItem(val user: SearchUser) : SearchResultItem
}

data class SearchUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val isPremium: Boolean,
)
