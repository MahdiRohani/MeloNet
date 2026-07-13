package com.melonet.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
    data object Search : BottomNavEntry<SearchRoute>(
        R.string.nav_search,
        Icons.Default.Search,
        SearchRoute,
    )

    data object Chat : BottomNavEntry<ConversationsRoute>(
        R.string.nav_chat,
        Icons.AutoMirrored.Filled.Chat,
        ConversationsRoute(),
    )

    data object Home : BottomNavEntry<HomeRoute>(
        R.string.nav_home,
        Icons.Default.Home,
        HomeRoute,
    )

    data object Downloads : BottomNavEntry<DownloadsRoute>(
        R.string.nav_downloads,
        Icons.Default.Download,
        DownloadsRoute,
    )

    data object Profile : BottomNavEntry<ProfileRoute>(
        R.string.nav_profile,
        Icons.Default.Person,
        ProfileRoute,
    )
}

// Home is intentionally kept in the centre of the bar.
val bottomNavEntries = listOf(
    BottomNavEntry.Search,
    BottomNavEntry.Chat,
    BottomNavEntry.Home,
    BottomNavEntry.Downloads,
    BottomNavEntry.Profile,
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
            val isSelected = entry.route.let { route ->
                currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
            }

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
