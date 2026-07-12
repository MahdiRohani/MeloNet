package com.melonet.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable data object SplashRoute
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object HomeRoute
@Serializable data object SearchRoute
@Serializable data object DownloadsRoute
@Serializable data object PlaylistsRoute
@Serializable data object ProfileRoute

@Serializable data object LikedSongsRoute
@Serializable data object RecentSongsRoute
@Serializable data object FollowingRoute

@Serializable
data class CatalogRoute(
    val listType: String,
    val filter: String? = null,
)

@Serializable
data class SongDetailRoute(val songId: String)

@Serializable
data class ArtistDetailRoute(val artistId: Int)

@Serializable
data class PlaylistDetailRoute(val playlistId: Int)

@Serializable
data class UserProfileRoute(val userId: Int)

@Serializable
data class PlayerRoute(val songId: String)

@Serializable
data class ChatRoute(val userId: Int)
