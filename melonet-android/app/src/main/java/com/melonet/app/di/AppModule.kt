package com.melonet.app.di

import com.melonet.app.BuildConfig
import com.melonet.app.core.common.DefaultDispatchersProvider
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.core.datastore.SettingsRepository
import com.melonet.app.core.network.AuthInterceptor
import com.melonet.app.core.network.TokenManager
import com.melonet.app.domain.repository.HomeRepository
import com.melonet.app.domain.repository.UserRepository
import com.melonet.app.feature.home.HomeViewModel
import com.melonet.app.feature.home.data.HomeRepositoryImpl
import com.melonet.app.feature.home.data.remote.HomeApi
import com.melonet.app.feature.profile.ProfileViewModel
import com.melonet.app.feature.profile.data.UserRepositoryImpl
import com.melonet.app.feature.profile.data.remote.AuthApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single<DispatchersProvider> { DefaultDispatchersProvider() }

    single { TokenManager(androidContext()) }
    single { SettingsRepository(androidContext()) }

    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(get()))
            .addInterceptor(logging)
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

    single<HomeRepository> { HomeRepositoryImpl(homeApi = get(), dispatchers = get()) }
    single<UserRepository> {
        UserRepositoryImpl(
            authApi = get(),
            settingsRepository = get(),
            tokenManager = get(),
            dispatchers = get()
        )
    }

    viewModel { HomeViewModel(homeRepository = get()) }
    viewModel { ProfileViewModel(userRepository = get()) }
}
