package com.melonet.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable data object SplashRoute
@Serializable data object HomeRoute
@Serializable data object SearchRoute
@Serializable data object DownloadsRoute
@Serializable data object PlaylistsRoute
@Serializable data object ProfileRoute

@Serializable
data class PlayerRoute(val songId: Int)

@Serializable
data class ChatRoute(val userId: Int)
