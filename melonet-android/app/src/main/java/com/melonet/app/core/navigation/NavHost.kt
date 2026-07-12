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
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloTopBar
import com.melonet.app.core.designsystem.component.MiniPlayerBar
import com.melonet.app.data.model.AuthState
import com.melonet.app.data.model.Song
import com.melonet.app.feature.auth.AuthViewModel
import com.melonet.app.feature.auth.LoginScreen
import com.melonet.app.feature.auth.LoginViewModel
import com.melonet.app.feature.auth.RegisterScreen
import com.melonet.app.feature.auth.RegisterViewModel
import com.melonet.app.feature.downloads.DownloadsScreen
import com.melonet.app.feature.downloads.DownloadsViewModel
import com.melonet.app.feature.home.HomeScreen
import com.melonet.app.feature.home.HomeViewModel
import com.melonet.app.feature.player.PlayerContract
import com.melonet.app.feature.player.PlayerScreen
import com.melonet.app.feature.player.PlayerViewModel
import com.melonet.app.feature.playlists.LibraryListType
import com.melonet.app.feature.playlists.LibrarySongsScreen
import com.melonet.app.feature.playlists.LibrarySongsViewModel
import com.melonet.app.feature.playlists.PlaylistDetailScreen
import com.melonet.app.feature.playlists.PlaylistDetailViewModel
import com.melonet.app.feature.playlists.PlaylistsScreen
import com.melonet.app.feature.playlists.PlaylistsViewModel
import com.melonet.app.feature.profile.ProfileScreen
import com.melonet.app.feature.profile.ProfileViewModel
import com.melonet.app.feature.search.SearchScreen
import com.melonet.app.feature.search.SearchViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MelonetMainScreen() {
    val navController = rememberNavController()
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val authViewModel: AuthViewModel = koinViewModel()
    val playerViewModel: PlayerViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val authState by authViewModel.authState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showAppShell = currentDestination?.let { destination ->
        !destination.hasRoute(SplashRoute::class) &&
            !destination.hasRoute(LoginRoute::class) &&
            !destination.hasRoute(RegisterRoute::class) &&
            !destination.hasRoute(PlayerRoute::class) &&
            !destination.hasRoute(ChatRoute::class)
    } == true

    val showMiniPlayer = showAppShell &&
        playerState.currentSong != null &&
        !currentDestination.hasRoute(PlayerRoute::class)

    val authenticatedUser = (authState as? AuthState.Authenticated)?.user

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val playQueue = queue.ifEmpty { listOf(song) }
        playerViewModel.handleEvent(PlayerContract.Event.PlaySong(song, playQueue))
        navController.navigate(PlayerRoute(songId = song.id))
    }

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
                    if (showMiniPlayer) {
                        val song = playerState.currentSong!!
                        val progress = if (playerState.durationMs > 0) {
                            playerState.positionMs.toFloat() / playerState.durationMs
                        } else {
                            0f
                        }
                        MiniPlayerBar(
                            title = song.title,
                            artist = song.artistName,
                            coverUrl = song.coverUrl,
                            isPlaying = playerState.isPlaying,
                            progress = progress,
                            onClick = { navController.navigate(PlayerRoute(songId = song.id)) },
                            onPlayPauseClick = {
                                playerViewModel.handleEvent(PlayerContract.Event.TogglePlayPause)
                            },
                        )
                    }
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
                    onPlaySong = { song, queue -> playSong(song, queue) },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }

            composable<SearchRoute> {
                val searchViewModel: SearchViewModel = koinViewModel()
                SearchScreen(
                    viewModel = searchViewModel,
                    onPlaySong = { song -> playSong(song) },
                    onNavigateToArtist = { artistId ->
                        navController.navigate(ArtistDetailRoute(artistId = artistId))
                    },
                    onNavigateToUser = { userId ->
                        navController.navigate(UserProfileRoute(userId = userId))
                    },
                )
            }
            composable<DownloadsRoute> {
                val downloadsViewModel: DownloadsViewModel = koinViewModel()
                DownloadsScreen(
                    viewModel = downloadsViewModel,
                    onPlaySong = { song -> playSong(song) },
                    onNavigateToProfile = { navController.navigate(ProfileRoute) },
                )
            }
            composable<PlaylistsRoute> {
                val playlistsViewModel: PlaylistsViewModel = koinViewModel()
                PlaylistsScreen(
                    viewModel = playlistsViewModel,
                    onNavigateToDetail = { playlistId ->
                        navController.navigate(PlaylistDetailRoute(playlistId = playlistId))
                    },
                    onNavigateToLiked = { navController.navigate(LikedSongsRoute) },
                    onNavigateToRecent = { navController.navigate(RecentSongsRoute) },
                )
            }
            composable<LikedSongsRoute> {
                val libraryViewModel: LibrarySongsViewModel = koinViewModel()
                LibrarySongsScreen(
                    listType = LibraryListType.LIKED,
                    viewModel = libraryViewModel,
                    onNavigateToPlayer = { songId -> navController.navigate(PlayerRoute(songId)) },
                    onPlayQueue = { startId, songs, shuffle ->
                        val queue = if (shuffle) songs.shuffled() else songs
                        val start = queue.find { it.id == startId } ?: queue.firstOrNull() ?: return@LibrarySongsScreen
                        playSong(start, queue)
                    },
                )
            }
            composable<RecentSongsRoute> {
                val libraryViewModel: LibrarySongsViewModel = koinViewModel()
                LibrarySongsScreen(
                    listType = LibraryListType.RECENT,
                    viewModel = libraryViewModel,
                    onNavigateToPlayer = { songId -> navController.navigate(PlayerRoute(songId)) },
                    onPlayQueue = { startId, songs, shuffle ->
                        val queue = if (shuffle) songs.shuffled() else songs
                        val start = queue.find { it.id == startId } ?: queue.firstOrNull() ?: return@LibrarySongsScreen
                        playSong(start, queue)
                    },
                )
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
                val detailViewModel: PlaylistDetailViewModel = koinViewModel()
                PlaylistDetailScreen(
                    playlistId = args.playlistId,
                    viewModel = detailViewModel,
                    onNavigateToPlayer = { songId -> navController.navigate(PlayerRoute(songId)) },
                    onPlayQueue = { startId, songs, shuffle ->
                        val queue = if (shuffle) songs.shuffled() else songs
                        val start = queue.find { it.id == startId } ?: queue.firstOrNull() ?: return@PlaylistDetailScreen
                        playSong(start, queue)
                    },
                )
            }

            composable<UserProfileRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<UserProfileRoute>()
                DummyScreen(
                    titleRes = R.string.placeholder_user_profile,
                    formatArg = args.userId,
                )
            }

            composable<ProfileRoute> {
                val profileViewModel: ProfileViewModel = koinViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLikedSongsClick = { navController.navigate(LikedSongsRoute) },
                    onMyPlaylistsClick = { navController.navigate(PlaylistsRoute) },
                )
            }

            composable<PlayerRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<PlayerRoute>()
                PlayerScreen(
                    viewModel = playerViewModel,
                    songId = args.songId,
                    onNavigateBack = { navController.popBackStack() },
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
