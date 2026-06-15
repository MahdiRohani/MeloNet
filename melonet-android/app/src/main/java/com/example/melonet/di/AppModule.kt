package com.example.melonet.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.melonet.data.local.SettingsRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.melonet.data.remote.MeloNetApi
import com.example.melonet.presentation.feature.home.data.HomeRepository
import com.example.melonet.presentation.feature.home.presentation.HomeViewModel
import com.example.melonet.presentation.feature.profile.data.UserRepository
import com.example.melonet.presentation.feature.profile.presentation.ProfileViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel

@RequiresApi(Build.VERSION_CODES.O)
val appModule = module{


    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    single {
        get<Retrofit>().create(MeloNetApi::class.java)
    }
    single { SettingsRepository(androidContext()) }
    single {
        UserRepository(
            api = get(),
            settingsRepository = get()
        )
    }
    viewModel { ProfileViewModel(userRepository = get()) }

    single { HomeRepository(api = get()) }
    viewModel { HomeViewModel(repository = get()) }
}