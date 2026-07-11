package com.melonet.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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

data class BottomNavItem<T : Any>(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val route: T
)

val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Default.Home, HomeRoute),
    BottomNavItem(R.string.nav_search, Icons.Default.Search, SearchRoute),
    BottomNavItem(R.string.nav_downloads, Icons.Default.Download, DownloadsRoute),
    BottomNavItem(R.string.nav_playlists, Icons.Default.QueueMusic, PlaylistsRoute),
    BottomNavItem(R.string.nav_profile, Icons.Default.Person, ProfileRoute)
)

@Composable
fun MelonetBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val title = stringResource(item.titleRes)
            val isSelected = currentDestination?.hierarchy?.any {
                it.hasRoute(item.route::class)
            } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(HomeRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = item.icon, contentDescription = title) },
                label = { Text(text = title) }
            )
        }
    }
}
