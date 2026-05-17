package com.example.melonet

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.melonet.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MeloNetApp : Application() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MeloNetApp)
            modules(
                appModule
            )
        }
    }
}