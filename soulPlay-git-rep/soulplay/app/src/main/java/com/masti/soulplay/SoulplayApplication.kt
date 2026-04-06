package com.masti.soulplay

import android.app.Application
import com.masti.soulplay.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SoulplayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SoulplayApplication)
            modules(appModule)
        }
    }
}
