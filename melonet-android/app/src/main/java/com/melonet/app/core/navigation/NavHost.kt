package com.melonet.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.melonet.app.R
import com.melonet.app.feature.home.HomeScreen
import com.melonet.app.feature.home.HomeViewModel
import com.melonet.app.feature.profile.ProfileScreen
import com.melonet.app.feature.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MelonetMainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { MelonetBottomNavigation(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<SplashRoute> {
                SplashScreen(onSplashFinished = {
                    navController.navigate(HomeRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                })
            }

            composable<HomeRoute> {
                val homeViewModel: HomeViewModel = koinViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onSongClick = { songId ->
                        navController.navigate(PlayerRoute(songId = songId))
                    }
                )
            }

            composable<SearchRoute> {
                DummyScreen(titleRes = R.string.placeholder_search_tab)
            }
            composable<DownloadsRoute> {
                DummyScreen(titleRes = R.string.placeholder_downloads_tab)
            }
            composable<PlaylistsRoute> {
                DummyScreen(titleRes = R.string.placeholder_playlists_tab)
            }

            composable<ProfileRoute> {
                val profileViewModel: ProfileViewModel = koinViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLikedSongsClick = { navController.navigate(PlaylistsRoute) },
                    onMyPlaylistsClick = { navController.navigate(PlaylistsRoute) }
                )
            }

            composable<PlayerRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<PlayerRoute>()
                DummyScreen(
                    titleRes = R.string.placeholder_player,
                    formatArg = args.songId
                )
            }

            composable<ChatRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ChatRoute>()
                DummyScreen(
                    titleRes = R.string.placeholder_chat,
                    formatArg = args.userId
                )
            }
        }
    }
}

@Composable
private fun DummyScreen(
    titleRes: Int,
    formatArg: Int? = null,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (formatArg != null) {
                stringResource(titleRes, formatArg)
            } else {
                stringResource(titleRes)
            }
        )
    }
}
