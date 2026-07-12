package com.melonet.app.di

import com.melonet.app.BuildConfig
import com.melonet.app.core.common.DefaultDispatchersProvider
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.network.AuthInterceptor
import com.melonet.app.core.network.TokenAuthenticator
import com.melonet.app.data.local.MeloNetDatabase
import com.melonet.app.data.local.SettingsRepository
import com.melonet.app.data.local.TokenManager
import com.melonet.app.data.remote.AuthApi
import com.melonet.app.data.remote.CatalogApi
import com.melonet.app.data.remote.HomeApi
import com.melonet.app.data.remote.LibraryApi
import com.melonet.app.data.remote.PlaylistApi
import com.melonet.app.data.remote.SearchApi
import com.melonet.app.data.remote.ChatApi
import com.melonet.app.data.remote.SocialApi
import com.melonet.app.data.repository.AuthRepository
import com.melonet.app.data.repository.HomeRepository
import com.melonet.app.data.repository.LibraryRepository
import androidx.work.WorkManager
import com.melonet.app.data.local.DownloadStorage
import com.melonet.app.data.repository.DownloadRepository
import com.melonet.app.data.repository.OfflineSongResolver
import com.melonet.app.data.repository.RoomOfflineSongResolver
import com.melonet.app.feature.downloads.DownloadsViewModel
import com.melonet.app.data.repository.PlayerRepository
import com.melonet.app.data.repository.PlaylistRepository
import com.melonet.app.data.repository.SearchRepository
import com.melonet.app.data.repository.UserRepository
import com.melonet.app.feature.auth.AuthViewModel
import com.melonet.app.feature.auth.LoginViewModel
import com.melonet.app.feature.auth.RegisterViewModel
import com.melonet.app.feature.home.HomeViewModel
import com.melonet.app.feature.player.PlaybackManager
import com.melonet.app.feature.player.PlayerViewModel
import com.melonet.app.feature.playlists.LibrarySongsViewModel
import com.melonet.app.feature.playlists.PlaylistDetailViewModel
import com.melonet.app.feature.playlists.PlaylistsViewModel
import com.melonet.app.data.realtime.ChatWebSocketClient
import com.melonet.app.data.repository.ChatRepository
import com.melonet.app.data.repository.SocialRepository
import com.melonet.app.feature.profile.EditProfileViewModel
import com.melonet.app.feature.profile.ProfileViewModel
import com.melonet.app.feature.settings.SettingsViewModel
import com.melonet.app.feature.chat.ChatViewModel
import com.melonet.app.feature.chat.ConversationsViewModel
import com.melonet.app.feature.social.UserListViewModel
import com.melonet.app.feature.social.UserProfileViewModel
import com.melonet.app.feature.search.SearchViewModel
import androidx.room.Room
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single<DispatchersProvider> { DefaultDispatchersProvider() }

    single { TokenManager(androidContext()) }
    single { SettingsRepository(androidContext()) }

    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single(named("noAuthClient")) {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single(named("noAuthRetrofit")) {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(get(named("noAuthClient")))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single(named("refreshAuthApi")) {
        get<Retrofit>(named("noAuthRetrofit")).create(AuthApi::class.java)
    }

    single {
        val tokenManager: TokenManager = get()
        val refreshAuthApi: AuthApi = get(named("refreshAuthApi"))
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .authenticator(TokenAuthenticator(tokenManager, refreshAuthApi))
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single { get<Retrofit>().create(HomeApi::class.java) }
    single { get<Retrofit>().create(AuthApi::class.java) }
    single { get<Retrofit>().create(SearchApi::class.java) }
    single { get<Retrofit>().create(CatalogApi::class.java) }
    single { get<Retrofit>().create(LibraryApi::class.java) }
    single { get<Retrofit>().create(PlaylistApi::class.java) }
    single { get<Retrofit>().create(SocialApi::class.java) }
    single { get<Retrofit>().create(ChatApi::class.java) }

    single {
        Room.databaseBuilder(androidContext(), MeloNetDatabase::class.java, "melonet.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<MeloNetDatabase>().searchHistoryDao() }
    single { get<MeloNetDatabase>().likedSongDao() }
    single { get<MeloNetDatabase>().playHistoryDao() }
    single { get<MeloNetDatabase>().downloadDao() }
    single { get<MeloNetDatabase>().chatMessageDao() }
    single { DownloadStorage(androidContext()) }
    single { WorkManager.getInstance(androidContext()) }

    single { HomeRepository(homeApi = get(), dispatchers = get()) }
    single {
        SearchRepository(
            searchApi = get(),
            searchHistoryDao = get(),
            dispatchers = get(),
        )
    }
    single {
        AuthRepository(
            authApi = get(),
            tokenManager = get(),
            settingsRepository = get(),
            dispatchers = get(),
        )
    }
    single {
        UserRepository(
            authApi = get(),
            settingsRepository = get(),
            tokenManager = get(),
            dispatchers = get(),
        )
    }
    single {
        PlayerRepository(
            catalogApi = get(),
            libraryApi = get(),
            offlineSongResolver = get(),
            dispatchers = get(),
        )
    }
    single { PlaylistRepository(playlistApi = get(), dispatchers = get()) }
    single {
        LibraryRepository(
            libraryApi = get(),
            likedSongDao = get(),
            playHistoryDao = get(),
            dispatchers = get(),
        )
    }
    single {
        DownloadRepository(
            downloadDao = get(),
            downloadStorage = get(),
            workManager = get(),
            dispatchers = get(),
        )
    }
    single { SocialRepository(socialApi = get(), dispatchers = get()) }
    single { ChatWebSocketClient(tokenManager = get(), dispatchers = get()) }
    single {
        ChatRepository(
            chatApi = get(),
            chatMessageDao = get(),
            webSocketClient = get(),
            playerRepository = get(),
            dispatchers = get(),
        )
    }
    single<OfflineSongResolver> { RoomOfflineSongResolver(downloadRepository = get()) }
    single { PlaybackManager(context = androidContext(), playerRepository = get()) }

    viewModel { AuthViewModel(authRepository = get()) }
    viewModel { LoginViewModel(authRepository = get()) }
    viewModel { RegisterViewModel(authRepository = get()) }
    viewModel { HomeViewModel(homeRepository = get()) }
    viewModel { ProfileViewModel(userRepository = get()) }
    viewModel { EditProfileViewModel(userRepository = get(), authRepository = get(), appContext = androidContext()) }
    viewModel { SettingsViewModel(settingsRepository = get(), authRepository = get()) }
    viewModel { UserProfileViewModel(socialRepository = get()) }
    viewModel { UserListViewModel(socialRepository = get()) }
    viewModel { ConversationsViewModel(chatRepository = get()) }
    viewModel { ChatViewModel(chatRepository = get(), socialRepository = get()) }
    viewModel { SearchViewModel(searchRepository = get()) }
    viewModel { PlayerViewModel(playbackManager = get(), downloadRepository = get(), userRepository = get()) }
    viewModel { PlaylistsViewModel(playlistRepository = get()) }
    viewModel { PlaylistDetailViewModel(playlistRepository = get()) }
    viewModel { LibrarySongsViewModel(libraryRepository = get()) }
    viewModel { DownloadsViewModel(downloadRepository = get(), userRepository = get()) }
}
