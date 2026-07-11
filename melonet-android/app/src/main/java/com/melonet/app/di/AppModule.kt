package com.melonet.app.di

import com.melonet.app.BuildConfig
import com.melonet.app.core.common.DefaultDispatchersProvider
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.network.AuthInterceptor
import com.melonet.app.core.network.TokenAuthenticator
import com.melonet.app.data.local.SettingsRepository
import com.melonet.app.data.local.TokenManager
import com.melonet.app.data.remote.AuthApi
import com.melonet.app.data.remote.HomeApi
import com.melonet.app.data.repository.AuthRepository
import com.melonet.app.data.repository.HomeRepository
import com.melonet.app.data.repository.UserRepository
import com.melonet.app.feature.auth.AuthViewModel
import com.melonet.app.feature.auth.LoginViewModel
import com.melonet.app.feature.auth.RegisterViewModel
import com.melonet.app.feature.home.HomeViewModel
import com.melonet.app.feature.profile.ProfileViewModel
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

    single { HomeRepository(homeApi = get(), dispatchers = get()) }
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
            dispatchers = get()
        )
    }

    viewModel { AuthViewModel(authRepository = get()) }
    viewModel { LoginViewModel(authRepository = get()) }
    viewModel { RegisterViewModel(authRepository = get()) }
    viewModel { HomeViewModel(homeRepository = get()) }
    viewModel { ProfileViewModel(userRepository = get()) }
}
