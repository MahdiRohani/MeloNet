package com.example.melonet.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem<T : Any>(
    val title: String,
    val icon: ImageVector,
    val route: T
)

val bottomNavItems = listOf(
    BottomNavItem("خانه", Icons.Default.Home, HomeRoute),
    BottomNavItem("جستجو", Icons.Default.Search, SearchRoute),
    BottomNavItem("دانلودها", Icons.Default.Download, DownloadsRoute),
    BottomNavItem("پلی‌لیست", Icons.Default.QueueMusic, PlaylistsRoute),
    BottomNavItem("پروفایل", Icons.Default.Person, ProfileRoute)
)

@Composable
fun MelonetBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination


    val hideBottomNav = currentDestination?.hasRoute(SplashRoute::class) == true ||
            currentDestination?.hasRoute(PlayerRoute::class) == true ||
            currentDestination?.hasRoute(ChatRoute::class) == true

    if (!hideBottomNav) {
        NavigationBar {
            bottomNavItems.forEach { item ->
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
                    icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                    label = { Text(text = item.title) }
                )
            }
        }
    }
}