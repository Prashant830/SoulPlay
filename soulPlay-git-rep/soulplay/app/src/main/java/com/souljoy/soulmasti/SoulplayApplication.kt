package com.souljoy.soulmasti

import android.app.Application
import com.souljoy.soulmasti.di.appModule
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
