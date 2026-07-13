package com.melonet.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.melonet.app.R

sealed class BottomNavEntry<T : Any>(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val route: T,
) {
    data object Home : BottomNavEntry<HomeRoute>(
        R.string.nav_home,
        Icons.Default.Home,
        HomeRoute,
    )

    data object Search : BottomNavEntry<SearchRoute>(
        R.string.nav_search,
        Icons.Default.Search,
        SearchRoute,
    )

    data object LocalMusic : BottomNavEntry<LocalMusicRoute>(
        R.string.nav_local_music,
        Icons.Default.PhoneAndroid,
        LocalMusicRoute,
    )

    data object Playlists : BottomNavEntry<PlaylistsRoute>(
        R.string.nav_playlists,
        Icons.Default.QueueMusic,
        PlaylistsRoute,
    )
}

val bottomNavEntries = listOf(
    BottomNavEntry.Home,
    BottomNavEntry.Search,
    BottomNavEntry.LocalMusic,
    BottomNavEntry.Playlists,
)

@Composable
fun MelonetBottomNavigation(
    navController: NavHostController,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavEntries.forEach { entry ->
            val title = stringResource(entry.titleRes)
            val isSelected = entry.route?.let { route ->
                currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
            } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(entry.route) {
                        popUpTo(HomeRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = entry.icon, contentDescription = title) },
                label = { Text(text = title) },
            )
        }
    }
}
