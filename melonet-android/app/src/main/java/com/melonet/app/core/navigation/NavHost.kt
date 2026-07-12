package com.melonet.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloTopBar
import com.melonet.app.data.model.AuthState
import com.melonet.app.feature.auth.AuthViewModel
import com.melonet.app.feature.auth.LoginScreen
import com.melonet.app.feature.auth.LoginViewModel
import com.melonet.app.feature.auth.RegisterScreen
import com.melonet.app.feature.auth.RegisterViewModel
import com.melonet.app.feature.home.HomeScreen
import com.melonet.app.feature.home.HomeViewModel
import com.melonet.app.feature.profile.ProfileScreen
import com.melonet.app.feature.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MelonetMainScreen() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.authState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showAppShell = currentDestination?.let { destination ->
        !destination.hasRoute(SplashRoute::class) &&
            !destination.hasRoute(LoginRoute::class) &&
            !destination.hasRoute(RegisterRoute::class) &&
            !destination.hasRoute(PlayerRoute::class) &&
            !destination.hasRoute(ChatRoute::class)
    } == true

    val authenticatedUser = (authState as? AuthState.Authenticated)?.user

    Scaffold(
        topBar = {
            if (showAppShell && authenticatedUser != null) {
                MeloTopBar(
                    avatarUrl = authenticatedUser.avatarUrl,
                    onAvatarClick = { navController.navigate(ProfileRoute) },
                    onNotificationsClick = { /* A9 */ },
                    onSettingsClick = { /* A9 */ },
                )
            }
        },
        bottomBar = {
            if (showAppShell) {
                Column {
                    // Mini player slot — wired in A6 (Player)
                    MelonetBottomNavigation(navController)
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<SplashRoute> {
                SplashScreen(
                    authState = authState,
                    onNavigateToAuth = {
                        navController.navigate(LoginRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(HomeRoute) {
                            popUpTo(SplashRoute) { inclusive = true }
                        }
                    },
                )
            }

            composable<LoginRoute> {
                val loginViewModel: LoginViewModel = koinViewModel()
                LoginScreen(
                    viewModel = loginViewModel,
                    onNavigateToMain = {
                        navController.navigate(HomeRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(RegisterRoute)
                    },
                )
            }

            composable<RegisterRoute> {
                val registerViewModel: RegisterViewModel = koinViewModel()
                RegisterScreen(
                    viewModel = registerViewModel,
                    onNavigateToMain = {
                        navController.navigate(HomeRoute) {
                            popUpTo(RegisterRoute) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                )
            }

            composable<HomeRoute> {
                val homeViewModel: HomeViewModel = koinViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onSongClick = { songId ->
                        navController.navigate(PlayerRoute(songId = songId))
                    },
                    onNavigate = { route ->
                        navController.navigate(route)
                    },
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
            composable<LikedSongsRoute> {
                DummyScreen(titleRes = R.string.profile_liked_songs)
            }
            composable<RecentSongsRoute> {
                DummyScreen(titleRes = R.string.home_quick_action_recent)
            }
            composable<FollowingRoute> {
                DummyScreen(titleRes = R.string.home_quick_action_following)
            }

            composable<CatalogRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<CatalogRoute>()
                val label = args.filter?.let { "${args.listType} ($it)" } ?: args.listType
                DummyScreen(
                    titleRes = R.string.placeholder_catalog,
                    formatArg = label,
                )
            }

            composable<SongDetailRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<SongDetailRoute>()
                DummyScreen(
                    titleRes = R.string.placeholder_song_detail,
                    formatArg = args.songId,
                )
            }

            composable<ArtistDetailRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ArtistDetailRoute>()
                DummyScreen(
                    titleRes = R.string.placeholder_artist_detail,
                    formatArg = args.artistId,
                )
            }

            composable<PlaylistDetailRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<PlaylistDetailRoute>()
                DummyScreen(
                    titleRes = R.string.placeholder_playlist_detail,
                    formatArg = args.playlistId,
                )
            }

            composable<ProfileRoute> {
                val profileViewModel: ProfileViewModel = koinViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLikedSongsClick = { navController.navigate(PlaylistsRoute) },
                    onMyPlaylistsClick = { navController.navigate(PlaylistsRoute) },
                )
            }

            composable<PlayerRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<PlayerRoute>()
                DummyScreen(
                    titleRes = R.string.placeholder_player,
                    formatArg = args.songId,
                )
            }

            composable<ChatRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ChatRoute>()
                DummyScreen(
                    titleRes = R.string.placeholder_chat,
                    formatArg = args.userId,
                )
            }
        }
    }
}

@Composable
private fun DummyScreen(
    titleRes: Int,
    formatArg: Any? = null,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (formatArg != null) {
                stringResource(titleRes, formatArg)
            } else {
                stringResource(titleRes)
            },
        )
    }
}
