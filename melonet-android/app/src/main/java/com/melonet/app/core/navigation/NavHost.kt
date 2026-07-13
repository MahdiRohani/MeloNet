package com.melonet.app.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.melonet.app.core.designsystem.component.OfflineBanner
import com.melonet.app.core.network.NetworkConnectivityMonitor
import com.melonet.app.data.model.AuthState
import com.melonet.app.data.model.Song
import com.melonet.app.feature.auth.AuthViewModel
import com.melonet.app.feature.auth.LoginScreen
import com.melonet.app.feature.auth.LoginViewModel
import com.melonet.app.feature.auth.RegisterScreen
import com.melonet.app.feature.auth.RegisterViewModel
import com.melonet.app.feature.artist.ArtistDetailScreen
import com.melonet.app.feature.artist.ArtistDetailViewModel
import com.melonet.app.feature.catalog.CatalogScreen
import com.melonet.app.feature.catalog.CatalogViewModel
import com.melonet.app.feature.following.FollowingScreen
import com.melonet.app.feature.following.FollowingViewModel
import com.melonet.app.feature.downloads.DownloadsScreen
import com.melonet.app.feature.downloads.DownloadsViewModel
import com.melonet.app.feature.home.HomeScreen
import com.melonet.app.feature.home.HomeViewModel
import com.melonet.app.feature.player.PlayerContract
import com.melonet.app.feature.player.PlayerScreen
import com.melonet.app.feature.player.PlayerViewModel
import com.melonet.app.feature.player.component.FloatingPlayerBubble
import com.melonet.app.feature.playlists.LibraryListType
import com.melonet.app.feature.playlists.LibrarySongsScreen
import com.melonet.app.feature.playlists.LibrarySongsViewModel
import com.melonet.app.feature.playlists.PlaylistDetailScreen
import com.melonet.app.feature.playlists.PlaylistDetailViewModel
import com.melonet.app.feature.playlists.PlaylistsScreen
import com.melonet.app.feature.playlists.PlaylistsViewModel
import com.melonet.app.feature.profile.EditProfileScreen
import com.melonet.app.feature.profile.EditProfileViewModel
import com.melonet.app.feature.profile.ProfileScreen
import com.melonet.app.feature.profile.ProfileViewModel
import com.melonet.app.feature.search.SearchScreen
import com.melonet.app.feature.search.SearchViewModel
import com.melonet.app.feature.settings.SettingsScreen
import com.melonet.app.feature.settings.SettingsViewModel
import com.melonet.app.feature.social.UserListScreen
import com.melonet.app.feature.social.UserListViewModel
import com.melonet.app.feature.social.UserProfileScreen
import com.melonet.app.feature.social.UserProfileViewModel
import com.melonet.app.data.model.UserListType
import com.melonet.app.feature.chat.ChatScreen
import com.melonet.app.feature.chat.ChatViewModel
import com.melonet.app.feature.chat.ConversationsScreen
import com.melonet.app.feature.chat.ConversationsViewModel
import com.melonet.app.data.repository.ChatRepository
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import com.melonet.app.core.permission.rememberAudioPermissionRequesterWithCallback
import com.melonet.app.feature.localmusic.LocalMusicScreen
import com.melonet.app.feature.localmusic.LocalMusicViewModel
import com.melonet.app.feature.playlists.AddSongsScreen
import com.melonet.app.feature.playlists.AddSongsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun MelonetMainScreen() {
    val navController = rememberNavController()
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val authViewModel: AuthViewModel = koinViewModel()
    val playerViewModel: PlayerViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val chatRepository: ChatRepository = koinInject()
    val networkMonitor: NetworkConnectivityMonitor = koinInject()
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
    val authState by authViewModel.authState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val unreadCount by chatRepository.unreadCount.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showAppShell = currentDestination?.let { destination ->
        !destination.hasRoute(SplashRoute::class) &&
            !destination.hasRoute(LoginRoute::class) &&
            !destination.hasRoute(RegisterRoute::class) &&
            !destination.hasRoute(PlayerRoute::class) &&
            !destination.hasRoute(ChatRoute::class) &&
            !destination.hasRoute(ConversationsRoute::class) &&
            !destination.hasRoute(SettingsRoute::class) &&
            !destination.hasRoute(EditProfileRoute::class) &&
            !destination.hasRoute(UserProfileRoute::class) &&
            !destination.hasRoute(UserListRoute::class) &&
            !destination.hasRoute(FollowingRoute::class) &&
            !destination.hasRoute(AddSongsRoute::class)
    } == true

    // Chat (ConversationsRoute) is a bottom-nav tab but keeps its own top bar,
    // so the bottom bar is shown there even though the shell top bar is not.
    val showBottomBar = showAppShell ||
        currentDestination?.hasRoute(ConversationsRoute::class) == true

    // When the player is minimized to a floating bubble, we hide the bottom
    // mini-player and show the draggable circular bubble instead.
    var bubbleVisible by remember { mutableStateOf(false) }
    // Drop the bubble if playback stops entirely.
    LaunchedEffect(playerState.currentSong) {
        if (playerState.currentSong == null) bubbleVisible = false
    }

    val showMiniPlayer = showAppShell &&
        playerState.currentSong != null &&
        !bubbleVisible &&
        !currentDestination.hasRoute(PlayerRoute::class)

    val authenticatedUser = (authState as? AuthState.Authenticated)?.user

    LaunchedEffect(authenticatedUser?.id) {
        val userId = authenticatedUser?.id
        if (userId != null) {
            chatRepository.setCurrentUserId(userId)
            chatRepository.connect()
            chatRepository.refreshUnreadCount()
        } else {
            chatRepository.disconnect()
        }
    }

    LaunchedEffect(authState, currentDestination) {
        if (authState is AuthState.Unauthenticated) {
            val onAuthScreen = currentDestination?.hasRoute(SplashRoute::class) == true ||
                currentDestination?.hasRoute(LoginRoute::class) == true ||
                currentDestination?.hasRoute(RegisterRoute::class) == true
            if (!onAuthScreen) {
                navController.navigate(LoginRoute) {
                    popUpTo(HomeRoute) { inclusive = true }
                }
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val playQueue = queue.ifEmpty { listOf(song) }
        playerViewModel.handleEvent(PlayerContract.Event.PlaySong(song, playQueue))
        navController.navigate(PlayerRoute(songId = song.id))
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val requestAudioPermission = rememberAudioPermissionRequesterWithCallback()

    Box(modifier = Modifier.fillMaxSize()) {
    MelonetNavigationDrawer(
        drawerState = drawerState,
        scope = scope,
        onNavigate = { route -> navController.navigate(route) },
    ) {
    Scaffold(
        topBar = {
            if (showAppShell && authenticatedUser != null) {
                Column {
                    if (!isOnline) {
                        OfflineBanner()
                    }
                    MeloTopBar(
                        onMenuClick = { scope.launch { drawerState.open() } },
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
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
            composable<LocalMusicRoute> {
                val localMusicViewModel: LocalMusicViewModel = koinViewModel()
                LocalMusicScreen(
                    viewModel = localMusicViewModel,
                    onPlaySong = { song, queue -> playSong(song, queue) },
                    requestAudioPermission = requestAudioPermission,
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
                val followingViewModel: FollowingViewModel = koinViewModel()
                val userId = authenticatedUser?.id ?: return@composable
                FollowingScreen(
                    userId = userId,
                    viewModel = followingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUser = { id -> navController.navigate(UserProfileRoute(userId = id)) },
                    onNavigateToArtist = { id -> navController.navigate(ArtistDetailRoute(artistId = id)) },
                )
            }

            composable<UserListRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<UserListRoute>()
                val listType = when (args.listType) {
                    "followers" -> UserListType.FOLLOWERS
                    else -> UserListType.FOLLOWING
                }
                val title = when (listType) {
                    UserListType.FOLLOWERS -> stringResource(R.string.social_followers)
                    UserListType.FOLLOWING -> stringResource(R.string.social_following)
                }
                val userListViewModel: UserListViewModel = koinViewModel()
                UserListScreen(
                    userId = args.userId,
                    listType = listType,
                    title = title,
                    viewModel = userListViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUser = { id -> navController.navigate(UserProfileRoute(userId = id)) },
                )
            }

            composable<CatalogRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<CatalogRoute>()
                val catalogViewModel: CatalogViewModel = koinViewModel()
                CatalogScreen(
                    listType = args.listType,
                    filter = args.filter,
                    viewModel = catalogViewModel,
                    onPlayQueue = { startId, songs, shuffle ->
                        val queue = if (shuffle) songs.shuffled() else songs
                        val start = queue.find { it.id == startId }
                            ?: queue.firstOrNull()
                            ?: return@CatalogScreen
                        playSong(start, queue)
                    },
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
                val artistViewModel: ArtistDetailViewModel = koinViewModel()
                ArtistDetailScreen(
                    artistId = args.artistId,
                    viewModel = artistViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPlayQueue = { startId, songs ->
                        val start = songs.find { it.id == startId }
                            ?: songs.firstOrNull()
                            ?: return@ArtistDetailScreen
                        playSong(start, songs)
                    },
                )
            }

            composable<PlaylistDetailRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<PlaylistDetailRoute>()
                val detailViewModel: PlaylistDetailViewModel = koinViewModel()
                val refreshPlaylist by backStackEntry.savedStateHandle
                    .getStateFlow("refresh_playlist", false)
                    .collectAsState()
                LaunchedEffect(refreshPlaylist) {
                    if (refreshPlaylist) {
                        detailViewModel.refreshSongs()
                        backStackEntry.savedStateHandle["refresh_playlist"] = false
                    }
                }
                PlaylistDetailScreen(
                    playlistId = args.playlistId,
                    viewModel = detailViewModel,
                    onNavigateToPlayer = { songId -> navController.navigate(PlayerRoute(songId)) },
                    onNavigateToAddSongs = { id -> navController.navigate(AddSongsRoute(playlistId = id)) },
                    onPlayQueue = { startId, songs, shuffle ->
                        val queue = if (shuffle) songs.shuffled() else songs
                        val start = queue.find { it.id == startId } ?: queue.firstOrNull() ?: return@PlaylistDetailScreen
                        playSong(start, queue)
                    },
                )
            }

            composable<UserProfileRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<UserProfileRoute>()
                val userProfileViewModel: UserProfileViewModel = koinViewModel()
                UserProfileScreen(
                    userId = args.userId,
                    viewModel = userProfileViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFollowers = { userId ->
                        navController.navigate(UserListRoute(userId = userId, listType = "followers"))
                    },
                    onNavigateToFollowing = { userId ->
                        navController.navigate(UserListRoute(userId = userId, listType = "following"))
                    },
                    onNavigateToPlaylist = { playlistId ->
                        navController.navigate(PlaylistDetailRoute(playlistId = playlistId))
                    },
                    onNavigateToChat = { userId ->
                        navController.navigate(ChatRoute(otherUserId = userId))
                    },
                )
            }

            composable<ConversationsRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ConversationsRoute>()
                val conversationsViewModel: ConversationsViewModel = koinViewModel()
                ConversationsScreen(
                    viewModel = conversationsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { conversationId, otherUserId, _ ->
                        navController.navigate(
                            ChatRoute(
                                otherUserId = otherUserId,
                                conversationId = conversationId,
                                shareSongId = args.shareSongId,
                            ),
                        )
                    },
                )
            }

            composable<AddSongsRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<AddSongsRoute>()
                val addSongsViewModel: AddSongsViewModel = koinViewModel()
                AddSongsScreen(
                    playlistId = args.playlistId,
                    viewModel = addSongsViewModel,
                    requestAudioPermission = requestAudioPermission,
                    onNavigateBack = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("refresh_playlist", true)
                        navController.popBackStack()
                    },
                )
            }

            composable<SettingsRoute> {
                val settingsViewModel: SettingsViewModel = koinViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(LoginRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    },
                )
            }

            composable<EditProfileRoute> {
                val editProfileViewModel: EditProfileViewModel = koinViewModel()
                EditProfileScreen(
                    viewModel = editProfileViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<ProfileRoute> {
                val profileViewModel: ProfileViewModel = koinViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onEditProfileClick = { navController.navigate(EditProfileRoute) },
                    onLikedSongsClick = { navController.navigate(LikedSongsRoute) },
                    onMyPlaylistsClick = { navController.navigate(PlaylistsRoute) },
                    onFollowingClick = { navController.navigate(FollowingRoute) },
                    onRecentlyPlayedClick = { navController.navigate(RecentSongsRoute) },
                    onLocalMusicClick = { navController.navigate(LocalMusicRoute) },
                    onDownloadsClick = { navController.navigate(DownloadsRoute) },
                )
            }

            composable<PlayerRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<PlayerRoute>()
                PlayerScreen(
                    viewModel = playerViewModel,
                    songId = args.songId,
                    onNavigateBack = { navController.popBackStack() },
                    onMinimize = {
                        bubbleVisible = true
                        navController.popBackStack()
                    },
                    onNavigateToArtist = { artistId ->
                        navController.navigate(ArtistDetailRoute(artistId = artistId))
                    },
                    onShareToChat = { shareSongId ->
                        navController.navigate(ConversationsRoute(shareSongId = shareSongId))
                    },
                )
            }

            composable<ChatRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ChatRoute>()
                val chatViewModel: ChatViewModel = koinViewModel()
                ChatScreen(
                    otherUserId = args.otherUserId,
                    conversationId = args.conversationId,
                    shareSongId = args.shareSongId,
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onPlaySong = { songId ->
                        playerViewModel.handleEvent(PlayerContract.Event.PlaySongId(songId))
                        navController.navigate(PlayerRoute(songId = songId))
                    },
                )
            }
        }
    }
    }

        if (bubbleVisible && showBottomBar) {
            playerState.currentSong?.let { song ->
                FloatingPlayerBubble(
                    coverUrl = song.coverUrl,
                    isPlaying = playerState.isPlaying,
                    onClick = {
                        bubbleVisible = false
                        navController.navigate(PlayerRoute(songId = song.id))
                    },
                    onTogglePlayPause = {
                        playerViewModel.handleEvent(PlayerContract.Event.TogglePlayPause)
                    },
                    modifier = Modifier.align(Alignment.TopStart),
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
