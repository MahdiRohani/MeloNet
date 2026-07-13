package com.melonet.app.feature.home

import com.melonet.app.core.navigation.ArtistDetailRoute
import com.melonet.app.core.navigation.CatalogRoute
import com.melonet.app.core.navigation.FollowingRoute
import com.melonet.app.core.navigation.LikedSongsRoute
import com.melonet.app.core.navigation.PlaylistsRoute
import com.melonet.app.core.navigation.RecentSongsRoute
import com.melonet.app.core.navigation.SearchRoute

sealed interface HomeDestination {
    data object Search : HomeDestination
    data class Catalog(val listType: String) : HomeDestination
    data class Category(val category: String) : HomeDestination
    data object Liked : HomeDestination
    data object Recent : HomeDestination
    data object Playlists : HomeDestination
    data object Following : HomeDestination
    data class Artist(val artistId: Int) : HomeDestination
}

object HomeNavigation {

    fun parseQuickActionTarget(target: String): HomeDestination? {
        val normalized = target.trim().trimStart('/')
        return when {
            normalized.equals("search", ignoreCase = true) -> HomeDestination.Search
            normalized.equals("catalog/popular", ignoreCase = true) ->
                HomeDestination.Catalog("popular")
            normalized.equals("catalog/new", ignoreCase = true) ->
                HomeDestination.Catalog("new")
            normalized.startsWith("songs?", ignoreCase = true) ->
                parseCategoryQuery(normalized.substringAfter('?'))
            "liked" in normalized.lowercase() || "favorite" in normalized.lowercase() ->
                HomeDestination.Liked
            "recent" in normalized.lowercase() || "history" in normalized.lowercase() ->
                HomeDestination.Recent
            "playlist" in normalized.lowercase() -> HomeDestination.Playlists
            "follow" in normalized.lowercase() || "artist" in normalized.lowercase() ->
                HomeDestination.Following
            else -> null
        }
    }

    fun parseSeeAllPath(path: String): HomeDestination? {
        val normalized = path.trim()
            .removePrefix("/api/")
            .removePrefix("api/")
        return parseQuickActionTarget(normalized)
    }

    private fun parseCategoryQuery(query: String): HomeDestination? {
        val category = query.split('&')
            .map { it.trim() }
            .firstOrNull { it.startsWith("category=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return HomeDestination.Category(category)
    }
}

fun HomeDestination.toRoute(): Any = when (this) {
    HomeDestination.Search -> SearchRoute
    is HomeDestination.Catalog -> CatalogRoute(listType = listType)
    is HomeDestination.Category -> CatalogRoute(listType = "category", filter = category)
    HomeDestination.Liked -> LikedSongsRoute
    HomeDestination.Recent -> RecentSongsRoute
    HomeDestination.Playlists -> PlaylistsRoute
    HomeDestination.Following -> FollowingRoute
    is HomeDestination.Artist -> ArtistDetailRoute(artistId = artistId)
}
